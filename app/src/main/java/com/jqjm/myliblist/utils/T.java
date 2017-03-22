package com.jqjm.myliblist.utils;

import android.content.Context;
import android.widget.Toast;

/**
 * Toast统一管理类
 * @author 曹加启
 */
public class T
{

	private T()
	{
		/* 不能被实例化*/
		throw new UnsupportedOperationException("Toast统一管理类，不能被实例化");
	}

	public static boolean isShow = true;

	/**
	 * 短时间显示Toast
	 * 
	 * @param context
	 * @param message
	 */
	public static void showShort(Context context, CharSequence message)
	{
		if (isShow)
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 短时间显示Toast
	 * 
	 * @param context
	 * @param message
	 */
	public static void showShort(Context context, int message)
	{
		if (isShow)
			Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
	}

	/**
	 * 长时间显示Toast
	 * 
	 * @param context
	 * @param message
	 */
	public static void showLong(Context context, CharSequence message)
	{
		if (isShow)
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * 长时间显示Toast
	 * 
	 * @param context
	 * @param message
	 */
	public static void showLong(Context context, int message)
	{
		if (isShow)
			Toast.makeText(context, message, Toast.LENGTH_LONG).show();
	}

	/**
	 * 自定义显示Toast时间
	 * 
	 * @param context
	 * @param message
	 * @param duration
	 */
	public static void show(Context context, CharSequence message, int duration)
	{
		if (isShow)
			Toast.makeText(context, message, duration).show();
	}

	/**
	 * 自定义显示Toast时间
	 * 
	 * @param context
	 * @param message
	 * @param duration
	 */
	public static void show(Context context, int message, int duration)
	{
		if (isShow)
			Toast.makeText(context, message, duration).show();
	}

}