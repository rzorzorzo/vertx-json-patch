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

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * User: gopi.vishwakarma Date: 05/08/14
 */
public class TestDataGenerator
{
	private static Random random = new Random();
	private static List<String> name = Arrays.asList("summers", "winters", "autumn", "spring", "rainy");
	private static List<Integer> age = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
	private static List<String> gender = Arrays.asList("male", "female");
	private static List<String> country = Arrays.asList("india", "aus", "nz", "sl", "rsa", "wi", "eng", "bang", "pak");
	private static List<String> friends = Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "a", "b", "c",
			"d", "e", "f", "g", "h", "i", "j", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j");

	public static Object generate(int count)
	{
		JsonArray Object = new JsonArray();
		for (int i = 0; i < count; i++)
		{
			JsonObject JsonObject = new JsonObject();
			JsonObject.put("name", name.get(random.nextInt(name.size())));
			JsonObject.put("age", age.get(random.nextInt(age.size())));
			JsonObject.put("gender", gender.get(random.nextInt(gender.size())));
			JsonArray countryNode = getJsonArray(country.subList(random.nextInt(country.size() / 2),
					(country.size() / 2) + random.nextInt(country.size() / 2)));
			JsonObject.put("country", countryNode);
			JsonArray friendNode = getJsonArray(friends.subList(random.nextInt(friends.size() / 2),
					(friends.size() / 2) + random.nextInt(friends.size() / 2)));
			JsonObject.put("friends", friendNode);
			Object.add(JsonObject);
		}
		return Object;
	}

	private static JsonArray getJsonArray(List<String> args)
	{
		JsonArray countryNode = new JsonArray();
		for (String arg : args)
		{
			countryNode.add(arg);
		}
		return countryNode;
	}
}
