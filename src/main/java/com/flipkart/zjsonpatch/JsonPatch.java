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

import java.util.EnumSet;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * User: gopi.vishwakarma Date: 31/07/14
 */
public final class JsonPatch
{

	private JsonPatch()
	{
	}

	private static Object getPatchAttr(JsonObject Object, String attr)
	{
		Object child = Object.getValue(attr);
		if (child == null && !Object.containsKey(attr))
			throw new InvalidJsonPatchException("Invalid JSON Patch payload (missing '" + attr + "' field)");
		return child;
	}

	private static Object getPatchAttrWithDefault(JsonObject Object, String attr, Object defaultValue)
	{
		Object child = Object.getValue(attr);
		if (child == null)
			return defaultValue;
		else
			return child;
	}

	private static void process(Object patch, JsonPatchProcessor processor, EnumSet<CompatibilityFlags> flags)
			throws InvalidJsonPatchException
	{

		if (!"array".equals(InternalUtils.getNodeType(patch)))
			throw new InvalidJsonPatchException("Invalid JSON Patch payload (not an array)");
		for (int i = 0; i < ((JsonArray) patch).size(); i++)
		{
			JsonObject Object = ((JsonArray) patch).getJsonObject(i);
			Operation operation = Operation.fromRfcName(getPatchAttr(Object, Constants.OP).toString());
			JsonPointer path = JsonPointer.parse(getPatchAttr(Object, Constants.PATH).toString());

			try
			{
				switch (operation)
				{
				case REMOVE:
				{
					processor.remove(path);
					break;
				}

				case ADD:
				{
					Object value;
					if (!flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS))
						value = getPatchAttr(Object, Constants.VALUE);
					else
						value = getPatchAttrWithDefault(Object, Constants.VALUE, null);
					processor.add(path, InternalUtils.deepCopy(value));
					break;
				}

				case REPLACE:
				{
					Object value;
					if (!flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS))
						value = getPatchAttr(Object, Constants.VALUE);
					else
						value = getPatchAttrWithDefault(Object, Constants.VALUE, null);
					processor.replace(path, InternalUtils.deepCopy(value));
					break;
				}

				case MOVE:
				{
					JsonPointer fromPath = JsonPointer.parse(getPatchAttr(Object, Constants.FROM).toString());
					processor.move(fromPath, path);
					break;
				}

				case COPY:
				{
					JsonPointer fromPath = JsonPointer.parse(getPatchAttr(Object, Constants.FROM).toString());
					processor.copy(fromPath, path);
					break;
				}

				case TEST:
				{
					Object value;
					// if (!flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS))
					// value = getPatchAttr(Object, Constants.VALUE);
					// else
					value = getPatchAttr(Object, Constants.VALUE);
					processor.test(path, InternalUtils.deepCopy(value));
					break;
				}
				}
			} catch (JsonPointerEvaluationException e)
			{
				throw new JsonPatchApplicationException(e.getMessage(), operation, e.getPath());
			}
		}
	}

	public static void validate(Object patch, EnumSet<CompatibilityFlags> flags) throws InvalidJsonPatchException
	{
		process(patch, NoopProcessor.INSTANCE, flags);
	}

	public static void validate(Object patch) throws InvalidJsonPatchException
	{
		validate(patch, CompatibilityFlags.defaults());
	}

	public static Object apply(Object patch, Object source, EnumSet<CompatibilityFlags> flags)
			throws JsonPatchApplicationException
	{
		CopyingApplyProcessor processor = new CopyingApplyProcessor(source, flags);
		process(patch, processor, flags);
		return processor.result();
	}

	public static Object apply(Object patch, Object source) throws JsonPatchApplicationException
	{
		return apply(patch, source, CompatibilityFlags.defaults());
	}

	public static void applyInPlace(Object patch, Object source)
	{
		applyInPlace(patch, source, CompatibilityFlags.defaults());
	}

	public static void applyInPlace(Object patch, Object source, EnumSet<CompatibilityFlags> flags)
	{
		InPlaceApplyProcessor processor = new InPlaceApplyProcessor(source, flags);
		process(patch, processor, flags);
	}
}
