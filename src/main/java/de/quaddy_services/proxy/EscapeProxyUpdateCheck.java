package de.quaddy_services.proxy;

import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import de.quaddy_services.proxy.logging.Logger;
import de.quaddy_services.proxy.logging.LoggerFactory;

/**
 * TODO
 * 
 * https://api.github.com/repos/quaddy-services/escape-from-intranet/releases/latest
 * html_url": "https://github.com/quaddy-services/escape-from-intranet/releases/tag/v1.4",
 */
public class EscapeProxyUpdateCheck {

	private static final Logger LOGGER = LoggerFactory.getLogger(EscapeProxyUpdateCheck.class);

	/**
	 */
	String updateUrl() {
		String aJsonString = readReleases();
		try {
			Map tempJson = jsonToMap(aJsonString);
			Object tempObject = tempJson.get("html_url");
			return tempObject == null ? null : tempObject.toString();
		} catch (ScriptException e) {
			LOGGER.error("Error", e);
			return null;
		}

	}

	/**
	 * 
	 */
	private String readReleases() {
		return null;
	}

	/**
	 * 
	 * Thanks to http://www.adam-bien.com/roller/abien/entry/converting_json_to_map_with
	 * @throws ScriptException 
	 */
	private Map jsonToMap(String aJsonString) throws ScriptException {
		ScriptEngineManager sem = new ScriptEngineManager();
		ScriptEngine engine = sem.getEngineByName("javascript");

		String json = new String(aJsonString);
		String script = "Java.asJSONCompatible(" + json + ")";
		Object result = engine.eval(script);

		Map contents = (Map) result;

		return contents;
	}
}
