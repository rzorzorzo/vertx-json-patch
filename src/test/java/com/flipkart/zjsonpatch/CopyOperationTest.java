package com.flipkart.zjsonpatch;

import java.io.IOException;
import java.util.Collection;

import org.junit.runners.Parameterized;

public class CopyOperationTest extends AbstractTest
{

	@Parameterized.Parameters
	public static Collection<PatchTestCase> data() throws IOException
	{
		return PatchTestCase.load("copy");
	}
}
