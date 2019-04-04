//// COPYRIGHT NOTICE (NOT TO BE REMOVED):
////
//// This file, or parts of it, or modified versions of it, may not be copied,
//// reproduced or transmitted in any form, including reprinting, translation,
//// photocopying or microfilming, or by any means, electronic, mechanical or
//// otherwise, or stored in a retrieval system, or used for any purpose, without
//// the prior written permission of all Owners unless it is explicitly marked as
//// having Classification `Public'.
////   Classification: Restricted.
////
//// Owners of this file give notice:
////   (c) Copyright 2010-2011 PES Software Engineering for Embedded Systems, TU Berlin
////
//// Authors:
////		Sabine Glesner
////		Robert Reicherdt
////		Elke Salecker
////		Volker Seeker
////		Joachim Kuhnert
//// 		Roman Busse
////
//// All rights, including copyrights, reserved.
////
//// This file contains or may contain restricted information and is UNPUBLISHED
//// PROPRIETARY SOURCE CODE OF THE Owners.  The Copyright Notice(s) above do not
//// evidence any actual or intended publication of such source code.  This file
//// is additionally subject to the conditions listed in the RESTRICTIONS file
//// and is with NO WARRANTY.
////
//// END OF COPYRIGHT NOTICE
//
// /***********UNUSED CLASS*************/
//
//package de.tu_berlin.pes.memo.parser;
//
//import org.hibernate.Session;
//import org.hibernate.SessionFactory;
//import org.hibernate.cfg.AnnotationConfiguration;
///**
// * @author reicherdt
// */
//public class SessionFactoryUtil {
//
//  /** The single instance of hibernate SessionFactory */
//  private static org.hibernate.SessionFactory sessionFactory;
//
//	/**
//	 * disable contructor to guaranty a single instance
//	 */
//	private SessionFactoryUtil() {
//	}
//
//	static{
//// Annotation and XML
//    sessionFactory = new AnnotationConfiguration().configure().buildSessionFactory();
//// XML only
////    sessionFactory = new Configuration().configure().buildSessionFactory();
//  }
//
//	public static SessionFactory getInstance() {
//		return sessionFactory;
//	}
//
//  /**
//   * Opens a session and will not bind it to a session context
//   * @return the session
//   */
//	public Session openSession() {
//		return sessionFactory.openSession();
//	}
//
//	/**
//   * Returns a session from the session context. If there is no session in the context it opens a session,
//   * stores it in the context and returns it.
//	 * This factory is intended to be used with a hibernate.cfg.xml
//	 * including the following property <property
//	 * name="current_session_context_class">thread</property> This would return
//	 * the current open session or if this does not exist, will create a new
//	 * session
//	 *
//	 * @return the session
//	 */
//	public Session getCurrentSession() {
//		return sessionFactory.getCurrentSession();
//	}
//
//  /**
//   * closes the session factory
//   */
//	public static void close(){
//		if (sessionFactory != null)
//			sessionFactory.close();
//		sessionFactory = null;
//
//	}
// }