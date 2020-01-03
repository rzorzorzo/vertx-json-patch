package com.flipkart.zjsonpatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

class InternalUtils
{

	public static Object deepCopy(Object target)
	{
		if (target == null)
			return null;
		if (target instanceof JsonArray)
			return ((JsonArray) target).copy();
		if (target instanceof JsonObject)
			return ((JsonObject) target).copy();
		return target;
	}

	static List<Object> toList(JsonArray input)
	{
		int size = input.size();
		List<Object> toReturn = new ArrayList<Object>(size);
		for (int i = 0; i < size; i++)
		{
			toReturn.add(input.getValue(i));
		}
		return toReturn;
	}

	static String getNodeType(Object node)
	{
		if (node == null)
			return "null";
		if (node instanceof JsonArray)
			return "array";
		if (node instanceof JsonObject)
			return "object";
		return "primitive";
	}

	static List<Object> longestCommonSubsequence(final List<Object> a, final List<Object> b)
	{
		if (a == null || b == null)
		{
			throw new NullPointerException("List must not be null for longestCommonSubsequence");
		}

		List<Object> toReturn = new LinkedList<Object>();

		int aSize = a.size();
		int bSize = b.size();
		int temp[][] = new int[aSize + 1][bSize + 1];

		for (int i = 1; i <= aSize; i++)
		{
			for (int j = 1; j <= bSize; j++)
			{
				if (i == 0 || j == 0)
				{
					temp[i][j] = 0;
				} else if (a.get(i - 1).equals(b.get(j - 1)))
				{
					temp[i][j] = temp[i - 1][j - 1] + 1;
				} else
				{
					temp[i][j] = Math.max(temp[i][j - 1], temp[i - 1][j]);
				}
			}
		}
		int i = aSize, j = bSize;
		while (i > 0 && j > 0)
		{
			if (a.get(i - 1).equals(b.get(j - 1)))
			{
				toReturn.add(a.get(i - 1));
				i--;
				j--;
			} else if (temp[i - 1][j] > temp[i][j - 1])
				i--;
			else
				j--;
		}
		Collections.reverse(toReturn);
		return toReturn;
	}

	public static boolean isEquals(Object value, Object value2)
	{
		if (value == null && value2 == null)
			return true;
		if (value == null || value2 == null)
			return false;
		return value.equals(value2);
	}
}
