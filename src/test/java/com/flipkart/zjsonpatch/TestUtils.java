package com.flipkart.zjsonpatch;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.Json;

public class TestUtils
{

	private TestUtils()
	{
	}

	public static Object loadResourceAsJsonNode(String path) throws IOException
	{
		String testData = loadFromResources(path);
		return Json.decodeValue(testData);
	}

	public static String loadFromResources(String path) throws IOException
	{
		InputStream resourceAsStream = PatchTestCase.class.getResourceAsStream(path);
		return IOUtils.toString(resourceAsStream, "UTF-8");
	}
}
