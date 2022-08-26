package net.runelite.rsb.internal;

import net.runelite.rsb.botLauncher.BotLite;
import net.runelite.rsb.script.Script;
import net.runelite.rsb.script.ScriptManifest;
import net.runelite.rsb.internal.listener.ScriptListener;

import java.util.*;

public class ScriptHandler {

	private final HashMap<Integer, Script> scripts = new HashMap<>();
	private final HashMap<Integer, Thread> scriptThreads = new HashMap<>();

	private final Set<ScriptListener> listeners = Collections.synchronizedSet(new HashSet<>());

	private final BotLite bot;

	public ScriptHandler(BotLite bot) {
		this.bot = bot;
	}

	public void init() {
	}

	public void addScriptListener(ScriptListener l) {
		listeners.add(l);
	}

	public void removeScriptListener(ScriptListener l) {
		listeners.remove(l);
	}

	private void addScriptToPool(Script ss, Thread t) {
		for (int off = 0; off < scripts.size(); ++off) {
			if (!scripts.containsKey(off)) {
				scripts.put(off, ss);
				ss.setID(off);
				scriptThreads.put(off, t);
				return;
			}
		}
		ss.setID(scripts.size());
		scripts.put(scripts.size(), ss);
		scriptThreads.put(scriptThreads.size(), t);
	}

	public BotLite getBot() {
		return bot;
	}

	public Map<Integer, Script> getRunningScripts() {
		return Collections.unmodifiableMap(scripts);
	}

	public void pauseScript(int id) {
		Script s = scripts.get(id);
		s.setPaused(!s.isPaused());
		if (s.isPaused()) {
			for (ScriptListener l : listeners) {
				l.scriptPaused(this, s);
			}
		} else {
			for (ScriptListener l : listeners) {
				l.scriptResumed(this, s);
			}
		}
	}

	public void stopScript(int id) {
		Script script = scripts.get(id);
		if (script != null) {
			script.deactivate(id);
			scripts.remove(id);
			scriptThreads.remove(id);
			for (ScriptListener l : listeners) {
				l.scriptStopped(this, script);
			}
		}
	}
    
	public void runScript(Script script) {
		script.init(bot.getMethodContext());
		for (ScriptListener l : listeners) {
			l.scriptStarted(this, script);
		}
		ScriptManifest prop = script.getClass().getAnnotation(ScriptManifest.class);
		Thread t = new Thread(script, "Script-" + prop.name());
		addScriptToPool(script, t);
		t.start();
	}

	public void stopAllScripts() {
		Set<Integer> theSet = scripts.keySet();
		int[] arr = new int[theSet.size()];
		int c = 0;
		for (int i : theSet) {
			arr[c] = i;
			c++;
		}
		for (int id : arr) {
			stopScript(id);
		}
	}

	public void stopScript() {
		Thread curThread = Thread.currentThread();
		for (int i = 0; i < scripts.size(); i++) {
			Script script = scripts.get(i);
			if (script != null && script.isRunning()) {
				if (scriptThreads.get(i) == curThread) {
					stopScript(i);
				}
			}
		}
		if (curThread == null) {
			throw new ThreadDeath();
		}
	}

	public void updateInput(BotLite bot, int mask) {
		for (ScriptListener l : listeners) {
			l.inputChanged(bot, mask);
		}
	}

}
