package de.tu_berlin.pes.memo.parser;

public class StandalonePreferenceStore {
	// db
	private String url;
	private String username;
	private String password;
	private String charEncoding;
	private Boolean useUnicode;
	private String driver;
	private String transaction_factory;
	private String query_factory;
	private String dialect;
	private Boolean show_sql;
	private String mapping_path;
	// matlab
	private String matlabpath;
	private Integer port;
	private Integer timeout;
	private String matlabip;
	private String matlabcontrolpath;
	private Integer retries;

	public synchronized Integer getRetries() {
		return retries;
	}

	public synchronized void setRetries(Integer retries) {
		this.retries = retries;
	}

	public synchronized String getMatlabcontrolpath() {
		return matlabcontrolpath;
	}

	public synchronized void setMatlabcontrolpath(String matlabcontrolpath) {
		this.matlabcontrolpath = matlabcontrolpath;
	}

	public synchronized String getMatlabip() {
		return matlabip;
	}

	public synchronized void setMatlabip(String matlabip) {
		this.matlabip = matlabip;
	}

	public synchronized String getUrl() {
		return url;
	}

	public synchronized void setUrl(String url) {
		this.url = url;
	}

	public synchronized String getUsername() {
		return username;
	}

	public synchronized void setUsername(String username) {
		this.username = username;
	}

	public synchronized String getPassword() {
		return password;
	}

	public synchronized void setPassword(String password) {
		this.password = password;
	}

	public synchronized String getCharEncoding() {
		return charEncoding;
	}

	public synchronized void setCharEncoding(String charEncoding) {
		this.charEncoding = charEncoding;
	}

	public synchronized Boolean getUseUnicode() {
		return useUnicode;
	}

	public synchronized void setUseUnicode(Boolean useUnicode) {
		this.useUnicode = useUnicode;
	}

	public synchronized String getDriver() {
		return driver;
	}

	public synchronized void setDriver(String driver) {
		this.driver = driver;
	}

	public synchronized String getTransaction_factory() {
		return transaction_factory;
	}

	public synchronized void setTransaction_factory(String transaction_factory) {
		this.transaction_factory = transaction_factory;
	}

	public synchronized String getQuery_factory() {
		return query_factory;
	}

	public synchronized void setQuery_factory(String query_factory) {
		this.query_factory = query_factory;
	}

	public synchronized String getDialect() {
		return dialect;
	}

	public synchronized void setDialect(String dialect) {
		this.dialect = dialect;
	}

	public synchronized Boolean getShow_sql() {
		return show_sql;
	}

	public synchronized void setShow_sql(Boolean show_sql) {
		this.show_sql = show_sql;
	}

	public synchronized String getMatlabpath() {
		return matlabpath;
	}

	public synchronized void setMatlabpath(String matlabpath) {
		this.matlabpath = matlabpath;
	}

	public synchronized Integer getPort() {
		return port;
	}

	public synchronized void setPort(Integer port) {
		this.port = port;
	}

	public synchronized Integer getTimeout() {
		return timeout;
	}

	public synchronized void setTimeout(Integer timeout) {
		this.timeout = timeout;
	}

	public synchronized String getMapping_path() {
		return mapping_path;
	}

	public synchronized void setMapping_path(String mapping_path) {
		this.mapping_path = mapping_path;
	}



	private static StandalonePreferenceStore _instance = null;

	private StandalonePreferenceStore() {
		
	}

	public boolean checkMatlab() {
		return matlabpath != null &&
				 port != null &&
				 timeout != null &&
				 matlabip != null &&
				 matlabcontrolpath != null &&
				 retries != null;
	}

	public boolean checkDb() {
		return url != null && username != null && password != null && charEncoding != null && useUnicode != null
				&& driver != null && transaction_factory != null && query_factory != null && dialect != null
				&& show_sql != null;
	}

	public static StandalonePreferenceStore getInstance() {
		if (_instance == null)
			_instance = new StandalonePreferenceStore();
		return _instance;
	}
}
