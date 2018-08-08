package com.commlibs.base.helper;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

/**
 * ### 原理
 * android中最重要的就是Handler机制了，简单来说Handler机制就是在一个死循环内部不断取走阻塞队列头部的Message，这个阻塞队列在主线程中是唯一的，当没有Message时，循环就阻塞，当一旦有Message时就立马被主线程取走并执行Message。
 * 查看android源码可以发现在ActivityThread中main方法
 * （main方法签名 ` public static void main(String[] args){}`,这个main方法是静态的，公有的，可以理解为应用的入口）
 * 最后执行了`Looper.loop();`，此方法内部是个死循环(for(;;)循环)，所以一般情况下主线程是不会退出的，除非抛出异常。
 * `queue.next();`就是从阻塞队列里取走头部的Message，当没有Message时主线程就会阻塞在这里，一有Message就会继续往下执行。
 * android的view绘制，事件分发，activity启动，activity的生命周期回调等等都是一个个的Message，
 * android会把这些Message插入到主线程中唯一的queue中，所有的消息都排队等待主线程的执行。
 * ActivityThread的main方法如下：
 * ```java
 * public static void main(String[] args) {
 * ...
 * Looper.prepareMainLooper();//创建主线程唯一的阻塞队列queue
 * ...
 * ActivityThread thread = new ActivityThread();
 * thread.attach(false);//执行初始化，往queue中添加Message等
 * ...
 * Looper.loop();//开启死循环，挨个执行Message
 * throw new RuntimeException("Main thread loop unexpectedly exited");
 * }
 * ```
 * `Looper.loop()`关键代码如下：
 * ```java
 * for (;;) {
 * Message msg = queue.next(); // might block
 * ...
 * msg.target.dispatchMessage(msg);//执行Message
 * ...
 * }
 * ```
 * android消息机制伪代码如下：
 * ```java
 * public class ActivityThread {
 * public static void main(String[]args){
 * Queue queue=new Queue();// 可以理解为一个加锁的，可以阻塞线程的ArrayList
 * queue.add(new Message(){
 * void run(){
 * ...
 * print("android 启动了，下一步该往queue中插入启动主Activity的Message了");
 * Message msg=getMessage4LaunchMainActivity();
 * queue.add(msg);
 * }
 * });
 * for(;;){//开始死循环，for之后的代码永远也得不到执行
 * Message  msg=queue.next();
 * msg.run();
 * }
 * }
 * }
 * ```
 * 看了上面的分析相信大家对android的消息机制很清楚了。
 * 关于Handler机制更多内容可以看这
 * [java工程实现Handler机制代码](https://android-notes.github.io/2016/12/03/5%E5%88%86%E9%92%9F%E5%AE%8C%E5%85%A8%E7%90%86%E8%A7%A3android-handler/)
 * 下面我们看一下Cockroach的核心代码
 * ```java
 * new Handler(Looper.getMainLooper()).post(new Runnable() {
 * @Override public void run() {
 * //主线程异常拦截
 * while (true) {
 * try {
 * Looper.loop();//主线程的异常会从这里抛出
 * } catch (Throwable e) {
 * }
 * }
 * }
 * });
 * sUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
 * //所有线程异常拦截，由于主线程的异常都被我们catch住了，所以下面的代码拦截到的都是子线程的异常
 * Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
 * @Override public void uncaughtException(Thread t, Throwable e) {
 * }
 * });
 * ```
 * 原理很简单，就是通过Handler往主线程的queue中添加一个Runnable，当主线程执行到该Runnable时，会进入我们的while死循环，如果while内部是空的就会导致代码卡在这里，最终导致ANR，但我们在while死循环中又调用了`Looper.loop()`，这就导致主线程又开始不断的读取queue中的Message并执行，这样就可以保证以后主线程的所有异常都会从我们手动调用的`Looper.loop()`处抛出，一旦抛出就会被try{}catch捕获，这样主线程就不会crash了，如果没有这个while的话那么主线程下次抛出异常时我们就又捕获不到了，这样APP就又crash了，所以我们要通过while让每次crash发生后都再次进入消息循环，while的作用仅限于每次主线程抛出异常后迫使主线程再次进入消息循环。我们可以用下面的伪代码来表示：
 * ```java
 * public class ActivityThread {
 * public static void main(String[]args){
 * Queue queue=new Queue();// 可以理解为一个加锁的，可以阻塞线程的ArrayList
 * ...
 * for(;;){//开始死循环，for之后的代码永远也得不到执行
 * Message  msg=queue.next();
 * //如果msg 是我们post的Runnable就会执行如下代码
 * //我们post的Runnable中的代码
 * while (true) {
 * try {
 * for(;;){//所有主线程的异常都会从msg.run()中抛出，所以我们加一个try{}catch来捕获所有主线程异常，捕获到后再次强迫进入循环，不断读取queue中消息并执行
 * Message  msg=queue.next();
 * msg.run();
 * }
 * } catch (Throwable e) {
 * }
 * //否则执行其他逻辑
 * }
 * }
 * ```
 * 为什么要通过new Handler.post方式而不是直接在主线程中任意位置执行
 * ` while (true) {
 * try {
 * Looper.loop();
 * } catch (Throwable e) {}
 * }`
 * 这是因为该方法是个死循环，若在主线程中，比如在Activity的onCreate中执行时会导致while后面的代码得不到执行，activity的生命周期也就不能完整执行，通过Handler.post方式可以保证不影响该条消息中后面的逻辑。
 * Created by wanjian on 2017/2/14.
 */

public final class CockroachCrash {

	public interface ExceptionHandler {

		void handlerException(Thread thread, Throwable throwable);
	}

	private CockroachCrash() {
	}

	private static ExceptionHandler sExceptionHandler;
	private static Thread.UncaughtExceptionHandler sUncaughtExceptionHandler;
	private static boolean sInstalled = false;//标记位，避免重复安装卸载

	/**
	 * handlerException内部建议手动try{  你的异常处理逻辑  }catch(Throwable e){ } ，以防handlerException内部再次抛出异常，导致循环调用handlerException
	 * @param context
	 */
	public static synchronized void install(final Context context) {
		CockroachCrash.install(new CockroachCrash.ExceptionHandler() {
			@Override
			public void handlerException(final Thread thread, final Throwable throwable) {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						try {
							Log.d("Cockroach", thread + "\n" + throwable.toString());
							throwable.printStackTrace();
							Toast.makeText(context, "Exception Happend\n" + thread + "\n" + throwable.toString(), Toast.LENGTH_SHORT).show();
						} catch (Throwable e) {

						}
					}
				});
			}
		});
	}


	/**
	 * 当主线程或子线程抛出异常时会调用exceptionHandler.handlerException(Thread thread, Throwable throwable)
	 * <p>
	 * exceptionHandler.handlerException可能运行在非UI线程中。
	 * <p>
	 * 若设置了Thread.setDefaultUncaughtExceptionHandler则可能无法捕获子线程异常。
	 * @param exceptionHandler
	 */
	public static synchronized void install(ExceptionHandler exceptionHandler) {
		if (sInstalled) {
			return;
		}
		sInstalled = true;
		sExceptionHandler = exceptionHandler;

		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {

				while (true) {
					try {
						Looper.loop();
					} catch (Throwable e) {
//                        Binder.clearCallingIdentity();
						if (e instanceof QuitCockroachException) {
							return;
						}
						if (sExceptionHandler != null) {
							sExceptionHandler.handlerException(Looper.getMainLooper().getThread(), e);
						}
					}
				}
			}
		});

		sUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				if (sExceptionHandler != null) {
					sExceptionHandler.handlerException(t, e);
				}
			}
		});

	}

	public static synchronized void uninstall() {
		if (!sInstalled) {
			return;
		}
		sInstalled = false;
		sExceptionHandler = null;
		//卸载后恢复默认的异常处理逻辑，否则主线程再次抛出异常后将导致ANR，并且无法捕获到异常位置
		Thread.setDefaultUncaughtExceptionHandler(sUncaughtExceptionHandler);
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				throw new QuitCockroachException("Quit Cockroach.....");//主线程抛出异常，迫使 while (true) {}结束
			}
		});

	}

	private final static class QuitCockroachException extends RuntimeException {
		public QuitCockroachException(String message) {
			super(message);
		}
	}

}
