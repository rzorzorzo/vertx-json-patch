package com.flipkart.zjsonpatch;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.EnumSet;

import org.junit.Test;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;

public class TestNodesEmissionTest
{

	private static EnumSet<DiffFlags> flags;

	static
	{
		flags = DiffFlags.defaults();
		flags.add(DiffFlags.EMIT_TEST_OPERATIONS);
	}

	@Test
	public void testNodeEmittedBeforeReplaceOperation() throws IOException
	{
		Object source = Json.decodeValue("{\"key\":\"original\"}");
		Object target = Json.decodeValue("{\"key\":\"replaced\"}");

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Object testNode = Json.decodeValue("{\"op\":\"test\",\"path\":\"/key\",\"value\":\"original\"}");
		assertEquals(2, diff.size());
		assertEquals(testNode, diff.iterator().next());
	}

	@Test
	public void testNodeEmittedBeforeCopyOperation() throws IOException
	{
		Object source = Json.decodeValue("{\"key\":\"original\"}");
		Object target = Json.decodeValue("{\"key\":\"original\", \"copied\":\"original\"}");

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Object testNode = Json.decodeValue("{\"op\":\"test\",\"path\":\"/key\",\"value\":\"original\"}");
		assertEquals(2, diff.size());
		assertEquals(testNode, diff.iterator().next());
	}

	@Test
	public void testNodeEmittedBeforeMoveOperation() throws IOException
	{
		Object source = Json.decodeValue("{\"key\":\"original\"}");
		Object target = Json.decodeValue("{\"moved\":\"original\"}");

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Object testNode = Json.decodeValue("{\"op\":\"test\",\"path\":\"/key\",\"value\":\"original\"}");
		assertEquals(2, diff.size());
		assertEquals(testNode, diff.iterator().next());
	}

	@Test
	public void testNodeEmittedBeforeRemoveOperation() throws IOException
	{
		Object source = Json.decodeValue("{\"key\":\"original\"}");
		Object target = Json.decodeValue("{}");

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Object testNode = Json.decodeValue("{\"op\":\"test\",\"path\":\"/key\",\"value\":\"original\"}");
		assertEquals(2, diff.size());
		assertEquals(testNode, diff.iterator().next());
	}

	@Test
	public void testNodeEmittedBeforeRemoveFromMiddleOfArray() throws IOException
	{
		Object source = Json.decodeValue("{\"key\":[1,2,3]}");
		Object target = Json.decodeValue("{\"key\":[1,3]}");

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Object testNode = Json.decodeValue("{\"op\":\"test\",\"path\":\"/key/1\",\"value\":2}");
		assertEquals(2, diff.size());
		assertEquals(testNode, diff.iterator().next());
	}

	@Test
	public void testNodeEmittedBeforeRemoveFromTailOfArray() throws IOException
	{
		Object source = Json.decodeValue("{\"key\":[1,2,3]}");
		Object target = Json.decodeValue("{\"key\":[1,2]}");

		JsonArray diff = JsonDiff.asJson(source, target, flags);

		Object testNode = Json.decodeValue("{\"op\":\"test\",\"path\":\"/key/2\",\"value\":3}");
		assertEquals(2, diff.size());
		assertEquals(testNode, diff.iterator().next());
	}
}
