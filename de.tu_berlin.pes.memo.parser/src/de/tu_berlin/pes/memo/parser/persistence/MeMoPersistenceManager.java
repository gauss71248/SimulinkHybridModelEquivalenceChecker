// COPYRIGHT NOTICE (NOT TO BE REMOVED):
//
// This file, or parts of it, or modified versions of it, may not be copied,
// reproduced or transmitted in any form, including reprinting, translation,
// photocopying or microfilming, or by any means, electronic, mechanical or
// otherwise, or stored in a retrieval system, or used for any purpose, without
// the prior written permission of all Owners unless it is explicitly marked as
// having Classification `Public'.
//   Classification: Restricted.
//
// Owners of this file give notice:
//   (c) Copyright 2010-2011 PES Software Engineering for Embedded Systems, TU Berlin
//
// Authors:
//		Sabine Glesner
//		Robert Reicherdt
//		Elke Salecker
//		Volker Seeker
//		Joachim Kuhnert
// 		Roman Busse
//
// All rights, including copyrights, reserved.
//
// This file contains or may contain restricted information and is UNPUBLISHED
// PROPRIETARY SOURCE CODE OF THE Owners.  The Copyright Notice(s) above do not
// evidence any actual or intended publication of such source code.  This file
// is additionally subject to the conditions listed in the RESTRICTIONS file
// and is with NO WARRANTY.
//
// END OF COPYRIGHT NOTICE

package de.tu_berlin.pes.memo.parser.persistence;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import org.conqat.lib.simulink.builder.MDLSection;
import org.eclipse.core.resources.IProject;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.tool.hbm2ddl.SchemaExport;

import de.tu_berlin.pes.memo.MeMoPlugin;
import de.tu_berlin.pes.memo.model.impl.Block;
import de.tu_berlin.pes.memo.model.impl.Model;
import de.tu_berlin.pes.memo.model.impl.ModelItem;
import de.tu_berlin.pes.memo.model.impl.SignalLine;
import de.tu_berlin.pes.memo.model.util.SimulinkParameterNames;
import de.tu_berlin.pes.memo.model.util.SimulinkSectionConstants;
import de.tu_berlin.pes.memo.parser.MeMoParserPlugin;
import de.tu_berlin.pes.memo.parser.ModelBuilder;
import de.tu_berlin.pes.memo.parser.mapping.Mapping;
import de.tu_berlin.pes.memo.preferences.MeMoPreferenceConstants;

/**
 * Class to handle all operations with the database.
 *
 * @author reicherdt, Joachim Kuhnert
 */
public class MeMoPersistenceManager {

	// /**
	// * for simple logging in a text file
	// */
	// final static Logger logger =
	// LoggerFactory.getLogger(MeMoPersistenceManager.class);

	/**
	 * The one instance to communicate with the database.
	 */
	private SessionFactory sessionFactory;

	/**
	 * The actual read model
	 */
	private Model model = null;
	/**
	 * Password for JDBC authentication.
	 */
	private String password;
	/**
	 * Username for JDBC authentication
	 */
	private String username;
	/**
	 * The complete path of the current database: parentURL + databaseName
	 */
	private String url;
	/**
	 * The path of the parent of the actual database
	 */
	private String parentURL;
	/**
	 * The name of the actual database
	 */
	private String databaseName;
	/**
	 * The actual configuration build of from properties.
	 */
	private Configuration configuration;
	/**
	 * The actual open session.
	 */
	private Session actualSession = null;

	private String lastLoadedModelDB;

	private static MeMoPersistenceManager instance = new MeMoPersistenceManager();

	// changes statistic of the Database
	private BigInteger inserts = new BigInteger("0");
	private BigInteger updates = new BigInteger("0");
	private BigInteger deletes = new BigInteger("0");

	private String characterEncoding;
	private Boolean useUnicode;
	private String driver;
	private String transaction_factory;
	private String query_factory;
	private String dialect;
	private Boolean show_sql;
	
	private String mapping_path;
	
	public void setMappingPath(String path) {
		this.mapping_path = path;
	}
	
	public void setConfiguration(String characterEncoding, Boolean useUnicode, String driver, String transaction_factory, String query_factory, String dialect, Boolean show_sql) {
		this.characterEncoding = characterEncoding;
		this.useUnicode = useUnicode;
		this.driver = driver;
		this.transaction_factory = transaction_factory;
		this.query_factory = query_factory;
		this.dialect = dialect;
		this.show_sql = show_sql;
	}
	
	public void setPassword(String password) {
		this.password = password;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * Creates a new <code>MeMoPersistenceManager</code>. It is recommend, that
	 * you use the <code>getInstance</code> Method instance, because every new
	 * MeMoPersistence-Instance had to load the model anew from the database even
	 * if the model is the same.
	 */
	public MeMoPersistenceManager() {
	}

	/**
	 * Only for internal use. Don't use Session outside of the
	 * MeMoPersistenceManager!
	 *
	 * Returns the session factory, creates one if necessary.
	 *
	 * @return The SessionFactory
	 */
	@Deprecated
	public SessionFactory getSessionFactory() {

		if ((sessionFactory == null) || sessionFactory.isClosed()) {
			sessionFactory = getConfiguration().buildSessionFactory();
		}
		return sessionFactory;
	}

	/**
	 * Delivers the actual session. Creates one, if necessary.
	 *
	 * @return The actual open session.
	 */
	public Session getSession() {
		if (actualSession == null) {
			actualSession = getSessionFactory().openSession();
		}
		return actualSession;
	}

	/**
	 * Starts a transaction.
	 *
	 * @return The started transaction.
	 */
	private Transaction beginTransaction() {
		return getSession().beginTransaction();
	}

	/**
	 * Closes the actual session and set it to null.
	 */
	private void closeSession() {
		if ((actualSession != null) && actualSession.isConnected()) {
			actualSession.flush();
			actualSession.close();
		}
		actualSession = null;
	}

	/**
	 * Saves the model in the Database. If any changes to the model are done,
	 * this function guarantees they will be stored.
	 *
	 * @return
	 */
	public boolean storeModel() {
		if (model != null) {
			int idcnt = model.nextID();
			Transaction tx = beginTransaction();
			getSession().persist(model);
			tx.commit();
			model.setIdCounter(idcnt);
			return true;
		}

		return false;
	}
	
	/**
	 * tries to randomize everything about a model,
	 * which can be used to reverse engineer it
	 * @param model
	 */
	private void randomizeNames(Model model) {
		int i = 0;
		for(Block b : model.getBlocks()) {
			String newName = "Block_"+i++;
			MeMoPlugin.out.println(b.getName()+" -> "+newName);
			b.setName(newName);
		}
		i = 0;
		for(SignalLine s : model.getSignalLines()) {
			s.setName("Signal_"+i++);
		}
		// TODO randomize other information
		// TODO note blocks
		for(Block b : model.getBlocks()) {
			if(b.getParameter().containsKey("port_label")) {
				System.out.println(b);
			}
			
			switch(b.getType()) {
				case "SubSystem":
					Map<String,String> params = b.getParameter();
					// reset name here, too
					params.put("Name", b.getName());
					continue;
				default: continue;
			}
		}
	}

	/**
	 * Converts the AST into the IR1 and stores the model in the Database.
	 *
	 * @param section
	 */
	public void saveModelToDB(MDLSection section, boolean matlabEvaluation,
			boolean generateDatabases, boolean randomizeNames) {
		ModelBuilder mdlBuilder = new ModelBuilder();
		try {

			if (generateDatabases) {
				getConfiguration(); // set properties for JDBC connection
				String modelName = section.getSubSections(SimulinkSectionConstants.MODEL_SECTION_TYPE)
						.get(0).getParameter(SimulinkParameterNames.MODEL_NAME_PARAMETER);
				String dbName = generateDBName(modelName);
				switchDatabase(dbName);
			}

			// there is a Model ?
			if (!getItemsByCriteria(Model.class).isEmpty()) {
				clearDatatbaseShema(true);
			}

			closeSession(); // new model -> new session. Close the old one

			model = mdlBuilder.buildModel(section, matlabEvaluation);
			if(randomizeNames) {
				// TODO: randomize model names if necessary her
				randomizeNames(model);
			}
			
			// persist the new model

			storeModel();

			// checkForLongParameters(m, 4096);

			MeMoPlugin.out.println("[INFO] Reassign Gotos...");

			mdlBuilder.reassignGotos(this, model);

			// storeModel();

			if (matlabEvaluation) {
				mdlBuilder.busMuxAnlaysis();
				storeModel();
			}

			MeMoPlugin.out.println("[INFO] Finished!");
			MeMoPlugin.out.println("Model Saved to DB");

		} catch (RuntimeException e) {
			MeMoPlugin.logException(e.toString(), e);
			MeMoPlugin.out.println(e.toString());
			model = null;
			throw e;
		}

	}

	/**
	 * Returns the model belonging to the actual database.
	 *
	 * @return The actual model.
	 */
	public Model getModel() {
		long cur_time = System.currentTimeMillis();
		// get the current changes count
		List ls = runSQLRequest("SELECT tup_inserted, tup_updated, tup_deleted FROM pg_stat_database "
				+ "WHERE datname = '" + databaseName + "'");

		BigInteger insertsNew = (BigInteger) ((Object[]) ls.get(0))[0];
		BigInteger updatesNew = (BigInteger) ((Object[]) ls.get(0))[1];
		BigInteger deletesNew = (BigInteger) ((Object[]) ls.get(0))[2];

		// if inserts, updates or deletes increase, model has changed
		if ((inserts.compareTo(insertsNew) < 0 // inserts < insertsNew
				)
				|| (updates.compareTo(updatesNew) < 0 // updates < updatesNew
				) || (deletes.compareTo(deletesNew) < 0)) { // deletes < deletesNew
			model = null;
		}

		// is the selected database the database of the current model?
		if ((databaseName != null) && !databaseName.equals(lastLoadedModelDB)) {
			model = null;
		}

		if (model == null) {
			MeMoPlugin.out.println("[INFO] Loading Model...");
			List result = new ArrayList<ModelItem>();

			// result = getItemsByCriteria(Model.class,
			// Restrictions.eq("isReference", false));
			result = getItemsByCriteria(Model.class);

			// DON'T CLOSE THE SESSION or lazy loading could fail

			if (!result.isEmpty()) {
				model = (Model) result.get(0);

				inserts = insertsNew;
				updates = updatesNew;
				deletes = deletesNew;
				lastLoadedModelDB = databaseName;
			}
		}
		long time = System.currentTimeMillis();
		long delta = time - cur_time;
		MeMoPlugin.out.println("[INFO] Time to load model: " + delta + "ms");
		return model;

	}

	/**
	 * Executes a SQL Querry via Hibernate
	 *
	 * @param sqlText
	 *           A string containing the query text
	 * @return A List containing the results. May be empty.
	 */
	public List runSQLRequest(String sqlText) {
		Transaction tx = null;
		List result = new ArrayList();
		try {

			tx = getSession().beginTransaction();
			SQLQuery q = getSession().createSQLQuery(sqlText);
			result = q.list();

			tx.commit();

		} catch (RuntimeException e) {
			if ((tx != null) && tx.isActive()) {
				try {
					// Second try catch as the rollback could fail as well
					tx.rollback();
					MeMoPlugin.out.println("ROLLBACK");
				} catch (HibernateException e1) {
					// logger.debug("Error rolling back transaction");
					result.add("Error rolling back transaction");
					result.add(e1);
					MeMoPlugin.logException(e1.toString(), e1);
				}
			}
			result.add(e);
			MeMoPlugin.logException(e.toString(), e);

		}

		getSession().flush();

		return result;
	}

	/**
	 * Functionality for the "Run Criteria"-Button.
	 *
	 * Creates and executes a criteria query, or as needed, a HQL query. Checks
	 * only for equality.
	 *
	 * @param isParameter
	 *           if true, a HQL query is needed
	 * @param clazz
	 *           the the class the result shall be
	 * @param property
	 *           the property which shall be checked
	 * @param value
	 *           the value to which the property will be compared
	 * @return A List of objects of type clazz where the property equals the
	 *         value
	 */
	public List runGUICriteriaRequest(boolean isParameter, Class clazz, String property, String value) {
		Transaction tx = null;
		List result = new ArrayList();
		try {

			tx = getSession().beginTransaction();

			// We have to use HQL :(
			if (isParameter) {
				result = getSession().createQuery(
						"from " + clazz.getSimpleName() + " b where b.parameter['" + property + "'] = '"
								+ value + "'").list();
			} else {
				// Use Criteria API
				// needs to be typed!!!
				Object v_obj = value;
				if (value.matches("\\d+")) {
					v_obj = Integer.parseInt(value);
				} else if (value.matches("\\d.\\d")) {
					v_obj = Double.parseDouble(value);
				}
				// create a crateria for a given class
				Criteria c = getSession().createCriteria(clazz);
				// add "where clause" if not empty
				if (!property.isEmpty() && !value.isEmpty()) {
					c.add(Restrictions.eq(property, v_obj));
				}
				// get results
				result = c.list();
			}
			tx.commit();
		} catch (RuntimeException e) {
			MeMoPlugin.logException(e.toString(), e);
			if ((tx != null) && tx.isActive()) {
				try {
					// Second try catch as the rollback could fail as well
					tx.rollback();
					MeMoPlugin.out.println("ROLLBACK");
				} catch (HibernateException e1) {
					MeMoPlugin.logException(e1.toString(), e1);
					// logger.debug("Error rolling back transaction");
					result.add("Error rolling back transaction");
					result.add(e1);
				}
				// throw again the first exception
				// throw e;
			}
			// result.add(e);
			// throw e;
		}

		return result;
	}

	/**
	 * Executes a criteria request
	 *
	 * @param c
	 *           The Criteria to execute
	 * @return The result list
	 */
	public List runCriteriaRequest(Criteria c) {
		Transaction tx = null;
		List result = new ArrayList();
		try {
			tx = getSession().beginTransaction();
			result = c.list();
			tx.commit();
		} catch (Exception e) {
			MeMoPlugin.err.println(e.toString());
		}
		return result;
	}

	/**
	 * Get Items by the hibernate criteria api.
	 *
	 * @param clazz
	 *           The the class the result shall be.
	 * @param resistictions
	 *           The criteria restrictions.
	 * @return The result list, where the items fulfill the restrictions.
	 */
	public List<ModelItem> getItemsByCriteria(Class clazz, List<SimpleExpression> resistictions) {
		Transaction tx = null;
		List<ModelItem> result = new ArrayList<ModelItem>();
		try {

			tx = getSession().beginTransaction();

			Criteria c = getSession().createCriteria(clazz);
			for (SimpleExpression ex : resistictions) {
				c.add(ex);
			}
			result = c.list();
			tx.commit();

		} catch (RuntimeException e) {
			MeMoPlugin.logException(e.toString(), e);
			if ((tx != null) && tx.isActive()) {
				try {
					// Second try catch as the rollback could fail as well
					tx.rollback();
				} catch (HibernateException e1) {
					MeMoPlugin.logException(e.toString(), e);
				}
			}
		}
		return result;

	}

	/**
	 * Get Items by the hibernate criteria api.
	 *
	 * @param clazz
	 *           The the class the result shall be.
	 * @param resistictions
	 *           The criteria restrictions.
	 * @return The result list, where the items fulfill the restrictions.
	 */
	public List getItemsByCriteria(Class clazz, SimpleExpression... resistictions) {
		Transaction tx = null;
		List<ModelItem> result = new ArrayList<ModelItem>();
		try {

			tx = getSession().beginTransaction();

			Criteria c = getSession().createCriteria(clazz);
			for (SimpleExpression ex : resistictions) {
				c.add(ex);
			}
			result = c.list();
			tx.commit();

		} catch (RuntimeException e) {
			MeMoPlugin.logException(e.toString(), e);
			if ((tx != null) && tx.isActive()) {
				try {
					// Second try catch as the rollback could fail as well
					tx.rollback();
				} catch (HibernateException e1) {
					MeMoPlugin.logException(e1.toString(), e1);
				}
			}
		}
		return result;

	}

	/**
	 * Creates and executes a HQL query.
	 *
	 * @param query
	 *           The query string to execute.
	 * @return The result list.
	 */
	@SuppressWarnings("unchecked")
	public List<ModelItem> getItemsByHQL(String query) {
		Transaction tx = null;
		List<ModelItem> result = new ArrayList<ModelItem>();
		try {

			tx = getSession().beginTransaction();

			result = getSession().createQuery(query).list();
			tx.commit();

		} catch (RuntimeException e) {
			MeMoPlugin.logException(e.toString(), e);
			if ((tx != null) && tx.isActive()) {
				try {
					// Second try catch as the rollback could fail as well
					tx.rollback();
				} catch (HibernateException e1) {
				}
				// throw again the first exception
			}
		}
		return result;

	}

	/**
	 * Drops all database tables and recreates them.
	 *
	 * @param recreateTables
	 *           create new table structure?
	 */
	public void clearDatatbaseShema(boolean recreateTables) {
		Configuration c = getConfiguration();
		SchemaExport s = new SchemaExport(c);
		s.drop(false, true);
		if (recreateTables) {
			s.create(false, true);
		}
	}

	/**
	 * Refreshes and returns the configuration.
	 *
	 * @return The refreshed configuration.
	 * @uml.property name="configuration"
	 */
	private Configuration getConfiguration() {
		if(MeMoPlugin.getDefault() != null) {
			username = MeMoPlugin.getDefault().getPreferenceStore()
					.getString(MeMoPreferenceConstants.HB_USER);
			password = MeMoPlugin.getDefault().getPreferenceStore()
					.getString(MeMoPreferenceConstants.HB_PASSWORD);
			url = MeMoPlugin.getDefault().getPreferenceStore()
					.getString(MeMoPreferenceConstants.HB_CONNECTION);
		} else if(username == null || password == null || url == null) {
			throw new RuntimeException("Username and/or password and/or url not set. If this is a cli application invokation, check your calls");
		}
		String[] urlparts = url.split("/");
		parentURL = urlparts[0] + "//" + urlparts[2] + "/"; // "jdbc:postgres://host:port/"
		for (int i = 3; i < (urlparts.length - 1); i++) {
			parentURL += urlparts[i] + "/";
		}
		if (urlparts.length > 3) {
			databaseName = urlparts[urlparts.length - 1];
		} else {
			databaseName = null;
		}

		configuration = new Configuration();

		/*
		 * thread is the short name for /*
		 * org.hibernate.context.ThreadLocalSessionContext and let Hibernate /*
		 * bind the session automatically to the thread
		 */
		configuration.setProperty("current_session_context_class", "thread");
		configuration.setProperty("hibernate.hbm2ddl.auto", "update");
		
		if(MeMoPlugin.getDefault() != null) {
			configuration.setProperty("hibernate.connection.characterEncoding", MeMoPlugin.getDefault()
					.getPreferenceStore().getString(MeMoPreferenceConstants.HB_CHARACTER_ENCODING));
			configuration.setProperty("hibernate.connection.useUnicode", MeMoPlugin.getDefault()
					.getPreferenceStore().getString(MeMoPreferenceConstants.HB_USE_UNICODE));
			configuration.setProperty("hibernate.connection.driver_class", MeMoPlugin.getDefault()
					.getPreferenceStore().getString(MeMoPreferenceConstants.HB_DRIVER));
			configuration.setProperty("connection.driver_class", MeMoPlugin.getDefault()
					.getPreferenceStore().getString(MeMoPreferenceConstants.HB_DRIVER));
			configuration.setProperty("transaction.factory_class", MeMoPlugin.getDefault()
					.getPreferenceStore().getString(MeMoPreferenceConstants.HB_TRANSACTION_FACTORY));
			configuration.setProperty("hibernate.query.factory_class", MeMoPlugin.getDefault()
					.getPreferenceStore().getString(MeMoPreferenceConstants.HB_QUERY_FACTORY));
			configuration.setProperty("hibernate.dialect", MeMoPlugin.getDefault().getPreferenceStore()
					.getString(MeMoPreferenceConstants.HB_DIALECT));
			configuration.setProperty("hibernate.show_sql", MeMoPlugin.getDefault().getPreferenceStore()
					.getString(MeMoPreferenceConstants.HB_SHOW_SQL));
		} else if(characterEncoding != null && useUnicode != null && driver != null && transaction_factory != null && query_factory != null && dialect != null && show_sql != null) {
			
		} else {
			throw new RuntimeException("Your DB configuration is incomplete");
		}
		
		configuration.setProperty("connection.url", url);
		configuration.setProperty("hibernate.connection.url", url);
		configuration.setProperty("connection.username", username);
		configuration.setProperty("hibernate.connection.username", username);
		configuration.setProperty("connection.password", password);
		configuration.setProperty("hibernate.connection.password", password);
		
		Enumeration<String> e = null;
		if(MeMoPlugin.getDefault() != null) {
			e = MeMoParserPlugin.getDefault().getBundle()
					.getEntryPaths("src/de/tu_berlin/pes/memo/parser/mapping");
		} else {
			// TODO: this needs a rework
			// url is null, if we try to run this code as standalone jar
			URL url = Mapping.class.getResource(".");
			if(url == null) {
				
			}
			
			Path folder = (new File(url.getPath())).toPath();
			Path base = folder.subpath(0, 5);
			ArrayList<String> list = new ArrayList<String>();
			for(File f: folder.toFile().listFiles()) {
				//hack
				list.add(f.getPath());
			}
			e = Collections.enumeration(list);
			
		}
		while (e.hasMoreElements()) {
			String s = e.nextElement();
			if (s.endsWith("hbm.xml")) {
				if(MeMoPlugin.getDefault() != null) {
					configuration.addResource(s);
				} else {
					configuration.addFile(s);
				}
			}
		}
		return configuration;
	}

	/**
	 * Lists all databases found under the current parent url / on the current
	 * server.
	 *
	 * @return The List of databases.
	 */
	public List<String> getDatabases() {
		ArrayList<String> result = new ArrayList<String>();
		getConfiguration(); // ensure parameters parentURL, username, password are
		// set
		try {
			Connection db = DriverManager.getConnection(parentURL, username, password);
			Statement sql = db.createStatement();
			ResultSet results = sql.executeQuery("select datname from pg_database");
			if (results != null) {
				while (results.next()) {
					String dbname = results.getString("datname");
					if (!(dbname.equals("template1") || dbname.equals("template0") || dbname
							.equals("postgres"))) {
						// list
						// the
						// standard
						// databases
						result.add(dbname);
					}
				}
			}
			results.close();
			db.close();
		} catch (SQLException e) {
			MeMoPlugin.logException(e.toString(), e);
		}

		return result;
	}

	/**
	 * Creates a database with the given name.
	 *
	 * @param dbName
	 *           The name of the database to create.
	 * @return If the operation was successful.
	 */
	public boolean createDatabase(String dbName) {
		getConfiguration(); // ensure parameters parentURL, username, password are
		// set
		try {
			MeMoPlugin.out.println("[INFO] Create Database  \"" + dbName + "\"");
			Connection db = DriverManager.getConnection(parentURL, username, password);
			Statement sql = db.createStatement();
			sql.executeUpdate("CREATE DATABASE " + dbName);
			db.close();
			return true;
		} catch (SQLException e) {
			MeMoPlugin.logException(e.toString(), e);
			return false;
		}
	}

	/**
	 * Drops a database.
	 *
	 * @param dbname
	 *           The name of the database to drop.
	 * @return If the operation was successful.
	 */
	public boolean dropDatabase(String dbname) {
		getConfiguration(); // ensure parameters parentURL, username, password are
		// set
		try {
			if (dbname.equals(databaseName) && (sessionFactory != null)) {
				sessionFactory.close(); // release all connections to the current
				// database
			}
			Connection db = DriverManager.getConnection(parentURL, username, password);
			Statement sql = db.createStatement();
			sql.executeUpdate("DROP DATABASE " + dbname);
			db.close();

			if (dbname.equals(databaseName)) { // if we drop the current database,
				// it couldn't be our actual
				// database any more
				databaseName = null;
				url = parentURL;
				model = null;
				closeSession();

				MeMoPlugin.getDefault().getPreferenceStore()
						.setValue(MeMoPreferenceConstants.HB_CONNECTION, url);
			}
			return true;
		} catch (SQLException e) {
			MeMoPlugin.out.println("[WARNING] Failed to delete Database " + dbname);
			return false;
		}
	}

	/**
	 * Switch to an other database. Creates the Database, if it not exists.
	 *
	 * @param dbName
	 *           The name of the database to switch to.
	 */
	public void switchDatabase(String dbName) {
		String realDbName = dbName.toLowerCase(Locale.ENGLISH); // postgres allows
		// only lower
		// case
		// databases,
		// important for
		// checking
		// existence
		getConfiguration(); // ensure parameters parentURL, username, password are
		// set
		boolean exists = false;
		for (String db : getDatabases()) {
			if (db.equals(realDbName)) {
				exists = true;
				break;
			}
		}

		if (!exists) {
			createDatabase(realDbName); // switch to a not existing database by
			// creating it first
		}

		// MeMoPlugin.out.println("[INFO] Switching to Database \"" + dbName +
		// "\""); // Don't spam user

		databaseName = realDbName;
		url = parentURL + realDbName;
		// model = null; // don't set model = null. Perhaps user switched away
		// from db an back -> eavl in getModel()
		closeSession();
		
		if(MeMoPlugin.getDefault() != null) {
			MeMoPlugin.getDefault().getPreferenceStore()
					.setValue(MeMoPreferenceConstants.HB_CONNECTION, url); // store
																								// current
																								// database
		}
		if (sessionFactory != null) {
			sessionFactory.close();
		}
	}

	/**
	 * Generates a name based on the name of the model to store of the form
	 * <code>modelNameInteger</code>, where modelName is the given model name in
	 * lower case and Integer is the highest integer already existing with this
	 * pattern for this model in the database plus one.
	 *
	 * @param modelName
	 *           The name to which a name shall be generated.
	 * @return modelName + Integer
	 */
	private String generateDBName(String modelName) {
		List<String> dbs = getDatabases();
		int maxIntFound = -1;
		for (int i = 0; i < dbs.size(); i++) {
			String current = dbs.get(i);
			if (current.startsWith(modelName.toLowerCase(Locale.ENGLISH))) {
				try {
					int newInt = Integer.parseInt(current.substring(modelName.length()));
					if (newInt > maxIntFound) {
						maxIntFound = newInt;
					}
				} catch (NumberFormatException e) {
					// NOP, just indicates it's no Int
				}

			}
		}
		return modelName.toLowerCase(Locale.ENGLISH) + ++maxIntFound;
	}

	/**
	 * Get the complete URL of the current active database:
	 * <code>parentURL/databaseName</code>
	 *
	 * @return the url
	 * @uml.property name="url"
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * Get the URL of the current database server without a specific database.
	 *
	 * @return the parentURL
	 * @uml.property name="parentURL"
	 */
	public String getParentURL() {
		return parentURL;
	}

	/**
	 * Get the name of the current database. Returns null, if no specific
	 * database is active.
	 *
	 * @return the databaseName, null if no current database is set.
	 * @uml.property name="databaseName"
	 */
	public String getDatabaseName() {
		return databaseName;
	}

	/**
	 * Returns the central <code>MeMoParserManager</code> instance. The
	 * <codeMeMoParserManager</code> is not singleton, you can create your own
	 * instances, but if you only use this Method you can use it as a singleton
	 * and gain performance advantages because a Model will not be loaded newly
	 * every time it is used.
	 *
	 * @return
	 */
	public static MeMoPersistenceManager getInstance() {
		return instance;
	}

	/* -------------------------- DEBUG --------------------- */
	//
	// private void checkForLongParameters(Model m, int size){
	// for(Block b : m.getBlocks()){
	// for(String paraName : b.getParameter().keySet()){
	// String value = b.getParameter().get(paraName);
	// if(value.length() > size){
	// MeMoPlugin.out.println("[DEBUG] " + b.getFullQualifiedName(true) +
	// " has an oversized Parameter (" + value.length() + ")" + paraName +
	// " with value " + value);
	// }
	// }
	// }
	// }

}
