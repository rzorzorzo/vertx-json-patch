/*
 * Copyright 2016 flipkart.com zjsonpatch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flipkart.zjsonpatch;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * User: gopi.vishwakarma Date: 30/07/14
 */

public final class JsonDiff
{

	private final List<Diff> diffs = new ArrayList<Diff>();
	private final EnumSet<DiffFlags> flags;

	private JsonDiff(EnumSet<DiffFlags> flags)
	{
		this.flags = flags.clone();
	}

	public static JsonArray asJson(final Object source, final Object target)
	{
		return asJson(source, target, DiffFlags.defaults());
	}

	public static JsonArray asJson(final Object source, final Object target, EnumSet<DiffFlags> flags)
	{
		JsonDiff diff = new JsonDiff(flags);

		// generating diffs in the order of their occurrence
		diff.generateDiffs(JsonPointer.ROOT, source, target);

		if (!flags.contains(DiffFlags.OMIT_MOVE_OPERATION))
			// Merging remove & add to move operation
			diff.introduceMoveOperation();

		if (!flags.contains(DiffFlags.OMIT_COPY_OPERATION))
			// Introduce copy operation
			diff.introduceCopyOperation(source, target);

		return diff.getObjects();
	}

	private static JsonPointer getMatchingValuePath(Map<Object, JsonPointer> unchangedValues, Object value)
	{
		return unchangedValues.get(value);
	}

	private void introduceCopyOperation(Object source, Object target)
	{
		Map<Object, JsonPointer> unchangedValues = getUnchangedPart(source, target);

		for (int i = 0; i < diffs.size(); i++)
		{
			Diff diff = diffs.get(i);
			if (Operation.ADD != diff.getOperation())
				continue;

			JsonPointer matchingValuePath = getMatchingValuePath(unchangedValues, diff.getValue());
			if (matchingValuePath != null && isAllowed(matchingValuePath, diff.getPath()))
			{
				// Matching value found; replace add with copy
				if (flags.contains(DiffFlags.EMIT_TEST_OPERATIONS))
				{
					// Prepend test node
					diffs.add(i, new Diff(Operation.TEST, matchingValuePath, diff.getValue()));
					i++;
				}
				diffs.set(i, new Diff(Operation.COPY, matchingValuePath, diff.getPath()));
			}
		}
	}

	private static boolean isNumber(String str)
	{
		int size = str.length();

		for (int i = 0; i < size; i++)
		{
			if (!Character.isDigit(str.charAt(i)))
			{
				return false;
			}
		}

		return size > 0;
	}

	// TODO this is quite unclear and needs some serious documentation
	private static boolean isAllowed(JsonPointer source, JsonPointer destination)
	{
		boolean isSame = source.equals(destination);
		int i = 0;
		int j = 0;
		// Hack to fix broken COPY operation, need better handling here
		while (i < source.size() && j < destination.size())
		{
			JsonPointer.RefToken srcValue = source.get(i);
			JsonPointer.RefToken dstValue = destination.get(j);
			String srcStr = srcValue.toString();
			String dstStr = dstValue.toString();
			if (isNumber(srcStr) && isNumber(dstStr))
			{

				if (srcStr.compareTo(dstStr) > 0)
				{
					return false;
				}
			}
			i++;
			j++;

		}
		return !isSame;
	}

	private static Map<Object, JsonPointer> getUnchangedPart(Object source, Object target)
	{
		Map<Object, JsonPointer> unchangedValues = new HashMap<Object, JsonPointer>();
		computeUnchangedValues(unchangedValues, JsonPointer.ROOT, source, target);
		return unchangedValues;
	}

	private static void computeUnchangedValues(Map<Object, JsonPointer> unchangedValues, JsonPointer path,
			Object source, Object target)
	{
		if (InternalUtils.isEquals(source, target))
		{
			if (!unchangedValues.containsKey(target))
			{
				unchangedValues.put(target, path);
			}
			return;
		}

		final String firstType = InternalUtils.getNodeType(source);
		final String secondType = InternalUtils.getNodeType(target);

		if (firstType.equals(secondType))
		{
			if ("object".equals(firstType))
				computeObject(unchangedValues, path, (JsonObject) source, (JsonObject) target);
			else if ("array".equals(firstType))
				computeArray(unchangedValues, path, (JsonArray) source, (JsonArray) target);
			/* else nothing */
		}
	}

	private static void computeArray(Map<Object, JsonPointer> unchangedValues, JsonPointer path, JsonArray source,
			JsonArray target)
	{
		final int size = Math.min(source.size(), target.size());

		for (int i = 0; i < size; i++)
		{
			JsonPointer currPath = path.append(i);
			computeUnchangedValues(unchangedValues, currPath, source.getValue(i), target.getValue(i));
		}
	}

	private static void computeObject(Map<Object, JsonPointer> unchangedValues, JsonPointer path, JsonObject source,
			JsonObject target)
	{
		final Set<String> firstFields = source.fieldNames();
		for (String name : firstFields)
		{
			if (target.containsKey(name))
			{
				JsonPointer currPath = path.append(name);
				computeUnchangedValues(unchangedValues, currPath, source.getValue(name), target.getValue(name));
			}
		}
	}

	/**
	 * This method merge 2 diffs ( remove then add, or vice versa ) with same value
	 * into one Move operation, all the core logic resides here only
	 */
	private void introduceMoveOperation()
	{
		for (int i = 0; i < diffs.size(); i++)
		{
			Diff diff1 = diffs.get(i);

			// if not remove OR add, move to next diff
			if (!(Operation.REMOVE == diff1.getOperation() || Operation.ADD == diff1.getOperation()))
			{
				continue;
			}

			for (int j = i + 1; j < diffs.size(); j++)
			{
				Diff diff2 = diffs.get(j);
				if (!InternalUtils.isEquals(diff1.getValue(), diff2.getValue()))
				{
					continue;
				}

				Diff moveDiff = null;
				if (Operation.REMOVE == diff1.getOperation() && Operation.ADD == diff2.getOperation())
				{
					JsonPointer relativePath = computeRelativePath(diff2.getPath(), i + 1, j - 1, diffs);
					moveDiff = new Diff(Operation.MOVE, diff1.getPath(), relativePath);

				} else if (Operation.ADD == diff1.getOperation() && Operation.REMOVE == diff2.getOperation())
				{
					JsonPointer relativePath = computeRelativePath(diff2.getPath(), i, j - 1, diffs); // diff1's add
																										// should also
																										// be considered
					moveDiff = new Diff(Operation.MOVE, relativePath, diff1.getPath());
				}
				if (moveDiff != null)
				{
					diffs.remove(j);
					diffs.set(i, moveDiff);
					break;
				}
			}
		}
	}

	// Note : only to be used for arrays
	// Finds the longest common Ancestor ending at Array
	private static JsonPointer computeRelativePath(JsonPointer path, int startIdx, int endIdx, List<Diff> diffs)
	{
		List<Integer> counters = new ArrayList<Integer>(path.size());
		for (int i = 0; i < path.size(); i++)
		{
			counters.add(0);
		}

		for (int i = startIdx; i <= endIdx; i++)
		{
			Diff diff = diffs.get(i);
			// Adjust relative path according to #ADD and #Remove
			if (Operation.ADD == diff.getOperation() || Operation.REMOVE == diff.getOperation())
			{
				updatePath(path, diff, counters);
			}
		}
		return updatePathWithCounters(counters, path);
	}

	private static JsonPointer updatePathWithCounters(List<Integer> counters, JsonPointer path)
	{
		List<JsonPointer.RefToken> tokens = path.decompose();
		for (int i = 0; i < counters.size(); i++)
		{
			int value = counters.get(i);
			if (value != 0)
			{
				int currValue = tokens.get(i).getIndex();
				tokens.set(i, new JsonPointer.RefToken(Integer.toString(currValue + value)));
			}
		}
		return new JsonPointer(tokens);
	}

	private static void updatePath(JsonPointer path, Diff pseudo, List<Integer> counters)
	{
		// find longest common prefix of both the paths

		if (pseudo.getPath().size() <= path.size())
		{
			int idx = -1;
			for (int i = 0; i < pseudo.getPath().size() - 1; i++)
			{
				if (pseudo.getPath().get(i).equals(path.get(i)))
				{
					idx = i;
				} else
				{
					break;
				}
			}
			if (idx == pseudo.getPath().size() - 2)
			{
				if (pseudo.getPath().get(pseudo.getPath().size() - 1).isArrayIndex())
				{
					updateCounters(pseudo, pseudo.getPath().size() - 1, counters);
				}
			}
		}
	}

	private static void updateCounters(Diff pseudo, int idx, List<Integer> counters)
	{
		if (Operation.ADD == pseudo.getOperation())
		{
			counters.set(idx, counters.get(idx) - 1);
		} else
		{
			if (Operation.REMOVE == pseudo.getOperation())
			{
				counters.set(idx, counters.get(idx) + 1);
			}
		}
	}

	private JsonArray getObjects()
	{
		final JsonArray patch = new JsonArray();
		for (Diff diff : diffs)
		{
			JsonObject Object = getObject(diff, flags);
			patch.add(Object);
		}
		return patch;
	}

	private static JsonObject getObject(Diff diff, EnumSet<DiffFlags> flags)
	{
		JsonObject Object = new JsonObject();
		Object.put(Constants.OP, diff.getOperation().rfcName());

		switch (diff.getOperation())
		{
		case MOVE:
		case COPY:
			Object.put(Constants.FROM, diff.getPath().toString()); // required {from} only in case of Move Operation
			Object.put(Constants.PATH, diff.getToPath().toString()); // destination Path
			break;

		case REMOVE:
			Object.put(Constants.PATH, diff.getPath().toString());
			if (!flags.contains(DiffFlags.OMIT_VALUE_ON_REMOVE))
				Object.put(Constants.VALUE, diff.getValue());
			break;

		case REPLACE:
			if (flags.contains(DiffFlags.ADD_ORIGINAL_VALUE_ON_REPLACE))
			{
				Object.put(Constants.FROM_VALUE, diff.getSrcValue());
			}
		case ADD:
		case TEST:
			Object.put(Constants.PATH, diff.getPath().toString());
			Object.put(Constants.VALUE, diff.getValue());
			break;

		default:
			// Safety net
			throw new IllegalArgumentException("Unknown operation specified:" + diff.getOperation());
		}

		return Object;
	}

	private void generateDiffs(JsonPointer path, Object source, Object target)
	{
		if (!InternalUtils.isEquals(source, target))
		{
			final String sourceType = InternalUtils.getNodeType(source);
			final String targetType = InternalUtils.getNodeType(target);

			if ("array".equals(sourceType) && "array".equals(targetType))
			{
				// both are arrays
				compareArray(path, (JsonArray) source, (JsonArray) target);
			} else if ("object".equals(sourceType) && "object".equals(targetType))
			{
				// both are json
				compareObjects(path, (JsonObject) source, (JsonObject) target);
			} else
			{
				// can be replaced
				if (flags.contains(DiffFlags.EMIT_TEST_OPERATIONS))
					diffs.add(new Diff(Operation.TEST, path, source));
				diffs.add(Diff.generateDiff(Operation.REPLACE, path, source, target));
			}
		}
	}

	private void compareArray(JsonPointer path, JsonArray source, JsonArray target)
	{
		List<Object> lcs = getLCS(source, target);
		int srcIdx = 0;
		int targetIdx = 0;
		int lcsIdx = 0;
		int srcSize = source.size();
		int targetSize = target.size();
		int lcsSize = lcs.size();

		int pos = 0;
		while (lcsIdx < lcsSize)
		{
			Object lcsNode = lcs.get(lcsIdx);
			Object srcNode = source.getValue(srcIdx);
			Object targetNode = target.getValue(targetIdx);

			if (lcsNode.equals(srcNode) && lcsNode.equals(targetNode))
			{ // Both are same as lcs node, nothing to do here
				srcIdx++;
				targetIdx++;
				lcsIdx++;
				pos++;
			} else
			{
				if (lcsNode.equals(srcNode))
				{ // src node is same as lcs, but not targetNode
					// addition
					JsonPointer currPath = path.append(pos);
					diffs.add(Diff.generateDiff(Operation.ADD, currPath, targetNode));
					pos++;
					targetIdx++;
				} else if (lcsNode.equals(targetNode))
				{ // targetNode node is same as lcs, but not src
					// removal,
					JsonPointer currPath = path.append(pos);
					if (flags.contains(DiffFlags.EMIT_TEST_OPERATIONS))
						diffs.add(new Diff(Operation.TEST, currPath, srcNode));
					diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, srcNode));
					srcIdx++;
				} else
				{
					JsonPointer currPath = path.append(pos);
					// both are unequal to lcs node
					generateDiffs(currPath, srcNode, targetNode);
					srcIdx++;
					targetIdx++;
					pos++;
				}
			}
		}

		while ((srcIdx < srcSize) && (targetIdx < targetSize))
		{
			Object srcNode = source.getValue(srcIdx);
			Object targetNode = target.getValue(targetIdx);
			JsonPointer currPath = path.append(pos);
			generateDiffs(currPath, srcNode, targetNode);
			srcIdx++;
			targetIdx++;
			pos++;
		}
		pos = addRemaining(path, target, pos, targetIdx, targetSize);
		removeRemaining(path, pos, srcIdx, srcSize, source);
	}

	private void removeRemaining(JsonPointer path, int pos, int srcIdx, int srcSize, JsonArray source)
	{
		while (srcIdx < srcSize)
		{
			JsonPointer currPath = path.append(pos);
			if (flags.contains(DiffFlags.EMIT_TEST_OPERATIONS))
				diffs.add(new Diff(Operation.TEST, currPath, source.getValue(srcIdx)));
			diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.getValue(srcIdx)));
			srcIdx++;
		}
	}

	private int addRemaining(JsonPointer path, JsonArray target, int pos, int targetIdx, int targetSize)
	{
		while (targetIdx < targetSize)
		{
			Object Object = target.getValue(targetIdx);
			JsonPointer currPath = path.append(pos);
			diffs.add(Diff.generateDiff(Operation.ADD, currPath, InternalUtils.deepCopy(Object)));
			pos++;
			targetIdx++;
		}
		return pos;
	}

	private void compareObjects(JsonPointer path, JsonObject source, JsonObject target)
	{
		Set<String> keysFromSrc = source.fieldNames();
		for (String key : keysFromSrc)
		{
			if (!target.containsKey(key))
			{
				// remove case
				JsonPointer currPath = path.append(key);
				if (flags.contains(DiffFlags.EMIT_TEST_OPERATIONS))
					diffs.add(new Diff(Operation.TEST, currPath, source.getValue(key)));
				diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.getValue(key)));
				continue;
			}
			JsonPointer currPath = path.append(key);
			generateDiffs(currPath, source.getValue(key), target.getValue(key));
		}
		Set<String> keysFromTarget = target.fieldNames();
		for (String key : keysFromTarget)
		{
			if (!source.containsKey(key))
			{
				// add case
				JsonPointer currPath = path.append(key);
				diffs.add(Diff.generateDiff(Operation.ADD, currPath, target.getValue(key)));
			}
		}
	}

	private static List<Object> getLCS(final Object first, final Object second)
	{
		return ListUtils.longestCommonSubsequence(InternalUtils.toList((JsonArray) first),
				InternalUtils.toList((JsonArray) second));
	}
}
