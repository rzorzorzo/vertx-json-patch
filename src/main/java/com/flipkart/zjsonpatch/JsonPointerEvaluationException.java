package com.flipkart.zjsonpatch;

public class JsonPointerEvaluationException extends Exception
{
	private final JsonPointer path;
	private final Object target;

	public JsonPointerEvaluationException(String message, JsonPointer path, Object target)
	{
		super(message);
		this.path = path;
		this.target = target;
	}

	public JsonPointer getPath()
	{
		return path;
	}

	public Object getTarget()
	{
		return target;
	}
}
