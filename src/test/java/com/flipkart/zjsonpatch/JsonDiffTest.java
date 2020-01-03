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
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Random;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Unit test
 */
public class JsonDiffTest
{
	private static JsonArray Object;

	@BeforeClass
	public static void beforeClass() throws IOException
	{
		String path = "/testdata/sample.json";
		InputStream resourceAsStream = JsonDiffTest.class.getResourceAsStream(path);
		String testData = IOUtils.toString(resourceAsStream, "UTF-8");
		Object = new JsonArray(testData);
	}

	@Test
	public void testSampleJsonDiff()
	{
		for (int i = 0; i < Object.size(); i++)
		{
			Object first = Object.getJsonObject(i).getValue("first");
			Object second = Object.getJsonObject(i).getValue("second");
			Object actualPatch = JsonDiff.asJson(first, second);
			Object secondPrime = JsonPatch.apply(actualPatch, first);
			Assert.assertEquals("JSON Patch not symmetrical [index=" + i + ", first=" + first + "]", second,
					secondPrime);
		}
	}

	@Test
	public void testGeneratedJsonDiff()
	{
		Random random = new Random();
		for (int i = 0; i < 1000; i++)
		{
			Object first = TestDataGenerator.generate(random.nextInt(10));
			Object second = TestDataGenerator.generate(random.nextInt(10));
			Object actualPatch = JsonDiff.asJson(first, second);
			Object secondPrime = JsonPatch.apply(actualPatch, first);
			Assert.assertEquals(second, secondPrime);
		}
	}

	@Test
	public void testRenderedRemoveOperationOmitsValueByDefault()
	{
		JsonObject source = new JsonObject();
		JsonObject target = new JsonObject();
		source.put("field", "value");

		JsonArray diff = JsonDiff.asJson(source, target);

		Assert.assertEquals(Operation.REMOVE.rfcName(), diff.getJsonObject(0).getValue("op").toString());
		Assert.assertEquals("/field", diff.getJsonObject(0).getValue("path").toString());
		Assert.assertNull(diff.getJsonObject(0).getValue("value"));
	}

	@Test
	public void testRenderedRemoveOperationRetainsValueIfOmitDiffFlagNotSet()
	{
		JsonObject source = new JsonObject();
		JsonObject target = new JsonObject();
		source.put("field", "value");

		EnumSet<DiffFlags> flags = DiffFlags.defaults().clone();
		Assert.assertTrue("Expected OMIT_VALUE_ON_REMOVE by default", flags.remove(DiffFlags.OMIT_VALUE_ON_REMOVE));
		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Assert.assertEquals(Operation.REMOVE.rfcName(), diff.getJsonObject(0).getValue("op").toString());
		Assert.assertEquals("/field", diff.getJsonObject(0).getValue("path").toString());
		Assert.assertEquals("value", diff.getJsonObject(0).getValue("value").toString());
	}

	@Test
	public void testRenderedOperationsExceptMoveAndCopy() throws Exception
	{
		Object source = Json.decodeValue("{\"age\": 10}");
		Object target = Json.decodeValue("{\"height\": 10}");

		EnumSet<DiffFlags> flags = DiffFlags.dontNormalizeOpIntoMoveAndCopy().clone(); // only have ADD, REMOVE,
																						// REPLACE, Don't normalize
																						// operations into MOVE & COPY

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		for (Object d : diff)
		{
			Assert.assertNotEquals(Operation.MOVE.rfcName(), ((JsonObject) d).getValue("op").toString());
			Assert.assertNotEquals(Operation.COPY.rfcName(), ((JsonObject) d).getValue("op").toString());
		}

		Object targetPrime = JsonPatch.apply(diff, source);
		Assert.assertEquals(target, targetPrime);
	}

	@Test
	public void testPath() throws Exception
	{
		Object source = Json.decodeValue("{\"profiles\":{\"abc\":[],\"def\":[{\"hello\":\"world\"}]}}");
		Object patch = Json.decodeValue(
				"[{\"op\":\"copy\",\"from\":\"/profiles/def/0\", \"path\":\"/profiles/def/0\"},{\"op\":\"replace\",\"path\":\"/profiles/def/0/hello\",\"value\":\"world2\"}]");

		Object target = JsonPatch.apply(patch, source);
		Object expected = Json
				.decodeValue("{\"profiles\":{\"abc\":[],\"def\":[{\"hello\":\"world2\"},{\"hello\":\"world\"}]}}");
		Assert.assertEquals(target, expected);
	}
}
