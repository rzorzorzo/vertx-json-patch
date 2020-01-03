package com.flipkart.zjsonpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class RFC6901Tests
{
	@Test
	void testRFC6901Compliance() throws IOException
	{
		JsonObject data = (JsonObject) TestUtils.loadResourceAsJsonNode("/rfc6901/data.json");
		Object testData = data.getValue("testData");

		JsonObject emptyJson = new JsonObject();
		Object patch = JsonDiff.asJson(emptyJson, testData);
		Object result = JsonPatch.apply(patch, emptyJson);
		assertEquals(testData, result);
	}
}
