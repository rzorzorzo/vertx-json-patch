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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.vertx.core.json.JsonObject;

public class PatchTestCase
{

	private final boolean operation;
	private final JsonObject node;
	private final String sourceFile;

	private PatchTestCase(boolean isOperation, JsonObject node, String sourceFile)
	{
		this.operation = isOperation;
		this.node = node;
		this.sourceFile = sourceFile;
	}

	public boolean isOperation()
	{
		return operation;
	}

	public JsonObject getNode()
	{
		return node;
	}

	public String getSourceFile()
	{
		return sourceFile;
	}

	public static Collection<PatchTestCase> load(String fileName) throws IOException
	{
		String path = "/testdata/" + fileName + ".json";
		JsonObject tree = (JsonObject) TestUtils.loadResourceAsJsonNode(path);

		List<PatchTestCase> result = new ArrayList<PatchTestCase>();
		for (Object node : tree.getJsonArray("errors"))
		{
			if (isEnabled((JsonObject) node))
			{
				result.add(new PatchTestCase(false, (JsonObject) node, path));
			}
		}
		for (Object node : tree.getJsonArray("ops"))
		{
			if (isEnabled((JsonObject) node))
			{
				result.add(new PatchTestCase(true, (JsonObject) node, path));
			}
		}
		return result;
	}

	private static boolean isEnabled(JsonObject node)
	{
		Boolean disabled = node.getBoolean("disabled");
		return (disabled == null || !disabled);
	}
}
