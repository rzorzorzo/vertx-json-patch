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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

import io.vertx.core.json.Json;

/**
 * @author ctranxuan (streamdata.io).
 */
public class MoveOperationTest extends AbstractTest
{

	@Parameterized.Parameters
	public static Collection<PatchTestCase> data() throws IOException
	{
		return PatchTestCase.load("move");
	}

	@Test
	public void testMoveValueGeneratedHasNoValue() throws IOException
	{
		Object Object1 = Json.decodeValue(
				"{ \"foo\": { \"bar\": \"baz\", \"waldo\": \"fred\" }, \"qux\": { \"corge\": \"grault\" } }");
		Object Object2 = Json.decodeValue(
				"{ \"foo\": { \"bar\": \"baz\" }, \"qux\": { \"corge\": \"grault\", \"thud\": \"fred\" } }");
		Object patch = Json.decodeValue("[{\"op\":\"move\",\"from\":\"/foo/waldo\",\"path\":\"/qux/thud\"}]");

		Object diff = JsonDiff.asJson(Object1, Object2);

		assertThat(diff, equalTo(patch));
	}

	@Test
	public void testMoveArrayGeneratedHasNoValue() throws IOException
	{
		Object Object1 = Json.decodeValue("{ \"foo\": [ \"all\", \"grass\", \"cows\", \"eat\" ] }");
		Object Object2 = Json.decodeValue("{ \"foo\": [ \"all\", \"cows\", \"eat\", \"grass\" ] }");
		Object patch = Json.decodeValue("[{\"op\":\"move\",\"from\":\"/foo/1\",\"path\":\"/foo/3\"}]");

		Object diff = JsonDiff.asJson(Object1, Object2);

		assertThat(diff, equalTo(patch));
	}
}
