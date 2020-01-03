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

import static com.flipkart.zjsonpatch.CompatibilityFlags.ALLOW_MISSING_TARGET_OBJECT_ON_REPLACE;
import static com.flipkart.zjsonpatch.CompatibilityFlags.MISSING_VALUES_AS_NULLS;
import static com.flipkart.zjsonpatch.CompatibilityFlags.REMOVE_NONE_EXISTING_ARRAY_ELEMENT;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.EnumSet;

import org.junit.Before;
import org.junit.Test;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class CompatibilityTest
{

	Object addNodeWithMissingValue;
	Object replaceNodeWithMissingValue;
	Object removeNoneExistingArrayElement;
	Object replaceNode;

	@Before
	public void setUp() throws Exception
	{
		addNodeWithMissingValue = Json.decodeValue("[{\"op\":\"add\",\"path\":\"/a\"}]");
		replaceNodeWithMissingValue = Json.decodeValue("[{\"op\":\"replace\",\"path\":\"/a\"}]");
		removeNoneExistingArrayElement = Json.decodeValue("[{\"op\": \"remove\",\"path\": \"/b/0\"}]");
		replaceNode = Json.decodeValue("[{\"op\":\"replace\",\"path\":\"/a\",\"value\":true}]");
	}

	@Test
	public void withFlagAddShouldTreatMissingValuesAsNulls() throws IOException
	{
		Object expected = Json.decodeValue("{\"a\":null}");
		Object result = JsonPatch.apply(addNodeWithMissingValue, new JsonObject(), EnumSet.of(MISSING_VALUES_AS_NULLS));
		assertThat(result, equalTo(expected));
	}

	@Test
	public void withFlagAddNodeWithMissingValueShouldValidateCorrectly()
	{
		JsonPatch.validate(addNodeWithMissingValue, EnumSet.of(MISSING_VALUES_AS_NULLS));
	}

	@Test
	public void withFlagReplaceShouldTreatMissingValuesAsNull() throws IOException
	{
		Object source = Json.decodeValue("{\"a\":\"test\"}");
		Object expected = Json.decodeValue("{\"a\":null}");
		Object result = JsonPatch.apply(replaceNodeWithMissingValue, source, EnumSet.of(MISSING_VALUES_AS_NULLS));
		assertThat(result, equalTo(expected));
	}

	@Test
	public void withFlagReplaceNodeWithMissingValueShouldValidateCorrectly()
	{
		JsonPatch.validate(addNodeWithMissingValue, EnumSet.of(MISSING_VALUES_AS_NULLS));
	}

	@Test
	public void withFlagIgnoreRemoveNoneExistingArrayElement() throws IOException
	{
		Object source = Json.decodeValue("{\"b\": []}");
		Object expected = Json.decodeValue("{\"b\": []}");
		Object result = JsonPatch.apply(removeNoneExistingArrayElement, source,
				EnumSet.of(REMOVE_NONE_EXISTING_ARRAY_ELEMENT));
		assertThat(result, equalTo(expected));
	}

	@Test
	public void withFlagReplaceShouldAddValueWhenMissingInTarget() throws Exception
	{
		Object expected = Json.decodeValue("{\"a\": true}");
		Object result = JsonPatch.apply(replaceNode, new JsonObject(),
				EnumSet.of(ALLOW_MISSING_TARGET_OBJECT_ON_REPLACE));
		assertThat(result, equalTo(expected));
	}
}
