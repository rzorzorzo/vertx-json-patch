package com.flipkart.zjsonpatch;

import java.util.EnumSet;

class CopyingApplyProcessor extends InPlaceApplyProcessor
{

	CopyingApplyProcessor(Object target)
	{
		this(target, CompatibilityFlags.defaults());
	}

	CopyingApplyProcessor(Object target, EnumSet<CompatibilityFlags> flags)
	{
		super(InternalUtils.deepCopy(target), flags);
	}
}
