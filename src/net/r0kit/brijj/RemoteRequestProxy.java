package net.r0kit.brijj;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public abstract class RemoteRequestProxy {
	protected HttpServletRequest request;
	protected HttpServletResponse response;
	protected Locale locale;
	protected HttpSession sess;
	protected ServletContext context;
	protected static Map<String, Class<? extends RemoteRequestProxy>> proxies = new HashMap<String, Class<? extends RemoteRequestProxy>>();

	/** registers classes to be exposed to Brijj.  If we could introspect and get a
	 * list of my subclasses, I wouldn't need to register those subclasses */
	public static void register(Class<? extends RemoteRequestProxy>... cls) {
		for (Class<? extends RemoteRequestProxy> cl : cls) {
			if (RemoteRequestProxy.class.isAssignableFrom(cl) || Remotable.class.isAssignableFrom(cl)) proxies.put(cl.getSimpleName(),
					cl);
			else throw new RuntimeException("cannot register " + cl);
		}
	}
	
	/** The constructor for a request proxy */
	public RemoteRequestProxy(HttpServletRequest r, HttpServletResponse s) {
		locale = new Locale("en");
		if (r != null) {
			context = r.getServletContext();
			sess = r.getServletContext().getEffectiveSessionTrackingModes() == null ? null : r.getSession();
			request = r;
			String lang = request.getHeader("accept-language");
			if (lang != null) locale = new Locale(lang.split(",")[0]);
		}
		response = s;
	}

	/** This list is used to generate the list of links for the test index page */
	public static Set<String> getProxyNames() {
		Set<String> hs = new TreeSet<String>();
		for (String s : proxies.keySet()) {
			if (RemoteRequestProxy.class.isAssignableFrom(proxies.get(s))) hs.add(s);
		}
		return hs;
	}
	
	/** Constructs the request proxy based on its name */
	public static RemoteRequestProxy getModule(String scriptName, HttpServletRequest request, HttpServletResponse response)
			throws ClassNotFoundException {
		Class<? extends RemoteRequestProxy> clazz = proxies.get(scriptName);
		if (clazz == null) throw new ClassNotFoundException(scriptName);
		Locale loca = new Locale("en");
		if (request != null) {
			String lang = request.getHeader("accept-language");
			if (lang != null) loca = new Locale(lang.split(",")[0]);
			if (null != request.getServletContext().getEffectiveSessionTrackingModes()) {
				request.getSession().setAttribute("locale", loca);
			}
		}
		try {
			return (RemoteRequestProxy) clazz.getConstructor(HttpServletRequest.class, HttpServletResponse.class).newInstance(
					request, response);
		} catch (InstantiationException rethrow) {
			throw new ClassNotFoundException(scriptName + ": " + rethrow.toString());
		} catch (IllegalAccessException ex) {
			throw new ClassNotFoundException("Illegal Access to default constructor on " + clazz.getName());
		} catch (NoSuchMethodException nsme) {
			throw new ClassNotFoundException("Unknown default constructor on " + clazz.getName());
		} catch (InvocationTargetException x) {
			throw new ClassNotFoundException(x.getTargetException().toString());
		}
	}
	@Override public String toString() {
		return this.getClass().getSimpleName() + "( ... )";
	}
	public String generateInterfaceScript(String csp, String scriptName) {
		RemoteRequestProxy module;
		try {
			module = RemoteRequestProxy.getModule(scriptName, null, null);
		} catch (ClassNotFoundException ignore) {
			System.err.println(ignore.toString());
			return "";
		}
		StringBuilder buffer = new StringBuilder();
		// defines the java classes in the global context
		buffer.append("\n(function() {  var _ = window; if (_." + scriptName + " == undefined) {\n    var p;");
		buffer.append("p = {}; p._path = '" + csp + "';\n");
		Method[] methods = module.getClass().getMethods();
		Arrays.sort(methods, new Comparator<Method>() {
			public int compare(Method a, Method b) {
				return a.getName().compareTo(b.getName());
			}
		});
		for (Method method : methods) {
			String methodName = method.getName();
			// Is it on the list of banned names
			if (Json.isReserved(methodName)) continue;
			Class<?>[] paramTypes = method.getParameterTypes();
			// Create the function definition
			buffer.append("p." + methodName + " = function(");
			for (int j = 0; j < paramTypes.length; j++)
				buffer.append("p").append(j).append(", ");
			buffer.append("callback) {  return brijj._execute(p._path, '");
			buffer.append(scriptName);
			buffer.append("', '");
			buffer.append(methodName);
			buffer.append("\', arguments); };\n");
		}
		buffer.append("    _." + scriptName + "=p; } })();\n");
		return buffer.toString();
	}
	
	
	  public static interface Remotable {
		    public Object toRemote();
		  }
		  public static Map<String, Object> toRemote(Object r) {
		    Map<String, Object> res = new HashMap<String, Object>();
		    for (Field f : r.getClass().getFields()) {
		      try {
		        res.put(f.getName(), f.get(r));
		      } catch (IllegalAccessException ignore) {}
		    }
		    return res;
		  }
		  public static class RemoteData implements Remotable {
		    public Map<String, Object> toRemote() {
		      return RemoteRequestProxy.toRemote(this);
		    }
		  }
		  
		  @Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME) public @interface Documentation {
		    String text() default "";
		  }
		  @Target(ElementType.PARAMETER) @Retention(RetentionPolicy.RUNTIME) public @interface Eg {
		    String value() default "";
		  }
	
}
