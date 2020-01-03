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

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.EnumSet;

import org.junit.Test;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

/**
 * User: holograph Date: 03/08/16
 */
public class ApiTest
{

	@Test
	public void applyInPlaceMutatesSource() throws Exception
	{
		Object patch = readTree("[{ \"op\": \"add\", \"path\": \"/b\", \"value\": \"b-value\" }]");
		JsonObject source = new JsonObject();
		JsonObject beforeApplication = (JsonObject) InternalUtils.deepCopy(source);
		JsonPatch.apply(patch, source);
		assertThat(source, is(beforeApplication));
	}

	@Test
	public void applyDoesNotMutateSource() throws Exception
	{
		Object patch = readTree("[{ \"op\": \"add\", \"path\": \"/b\", \"value\": \"b-value\" }]");
		JsonObject source = new JsonObject();
		JsonPatch.applyInPlace(patch, source);
		assertThat(source.getValue("b").toString(), is("b-value"));
	}

	@Test
	public void applyDoesNotMutateSource2() throws Exception
	{
		Object patch = readTree("[{ \"op\": \"add\", \"path\": \"/b\", \"value\": \"b-value\" }]");
		JsonObject source = new JsonObject();
		JsonObject beforeApplication = (JsonObject) InternalUtils.deepCopy(source);
		JsonPatch.apply(patch, source);
		assertThat(source, is(beforeApplication));
	}

	@Test
	public void applyInPlaceMutatesSourceWithCompatibilityFlags() throws Exception
	{
		Object patch = readTree("[{ \"op\": \"add\", \"path\": \"/b\" }]");
		JsonObject source = new JsonObject();
		JsonPatch.applyInPlace(patch, source, EnumSet.of(CompatibilityFlags.MISSING_VALUES_AS_NULLS));
		assertTrue(source.getValue("b") == null);
	}

	@Test(expected = Exception.class)
	public void applyingNonArrayPatchShouldThrowAnException() throws IOException
	{
		Object invalid = Json.decodeValue("{\"not\": \"a patch\"}");
		Object to = readTree("{\"a\":1}");
		JsonPatch.apply(invalid, to);
	}

	@Test(expected = Exception.class)
	public void applyingAnInvalidArrayShouldThrowAnException() throws IOException
	{
		Object invalid = readTree("[1, 2, 3, 4, 5]");
		Object to = readTree("{\"a\":1}");
		JsonPatch.apply(invalid, to);
	}

	@Test(expected = Exception.class)
	public void applyingAPatchWithAnInvalidOperationShouldThrowAnException() throws IOException
	{
		Object invalid = readTree("[{\"op\": \"what\"}]");
		Object to = readTree("{\"a\":1}");
		JsonPatch.apply(invalid, to);
	}

	@Test(expected = Exception.class)
	public void validatingNonArrayPatchShouldThrowAnException() throws IOException
	{
		Object invalid = readTree("{\"not\": \"a patch\"}");
		JsonPatch.validate(invalid);
	}

	@Test(expected = Exception.class)
	public void validatingAnInvalidArrayShouldThrowAnException() throws IOException
	{
		Object invalid = readTree("[1, 2, 3, 4, 5]");
		JsonPatch.validate(invalid);
	}

	@Test(expected = Exception.class)
	public void validatingAPatchWithAnInvalidOperationShouldThrowAnException() throws IOException
	{
		Object invalid = readTree("[{\"op\": \"what\"}]");
		JsonPatch.validate(invalid);
	}

	private static Object readTree(String jsonString) throws IOException
	{
		return Json.decodeValue(jsonString);
	}

}
