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

//package de.tu_berlin.pes.memo.parser.matlab;
//
//import com.modelengineers.jmbridge.api.MatlabConnection;
//import com.modelengineers.jmbridge.lg.CommandPacket;
//import com.modelengineers.jmbridge.lg.ErrorPacket;
//import com.modelengineers.jmbridge.lg.MatlabConnectionPacketListener;
//import com.modelengineers.jmbridge.lg.MatlabException;
//import com.modelengineers.jmbridge.lg.ResponsePacket;
//
//public class PortMatlabConnectorPacketListener implements
//		MatlabConnectionPacketListener {
//
//	private boolean gotResponse = false;
//	private boolean waitingForResponse = false;
//	private Object response = null;
//	private String object = "";
//	private String parameter = "";
//	private MatlabConnector matlab;
//
//	@SuppressWarnings("unused")
//	private PortMatlabConnectorPacketListener(){}
//
//	public PortMatlabConnectorPacketListener(MatlabConnector matlab){
//		this.matlab = matlab;
//	}
//
//
//	@Override
//	public void commandPacketReceived(MatlabConnection sender, CommandPacket commandPacket) {
//		MeMoPlugin.err.println("command packet received");
//	}
//
//	@Override
//	public void disconnectPacketReceived(MatlabConnection sender) {
//		MeMoPlugin.err.println("disconnect packet received");
//	}
//
//	@Override
//	public void errorPacketReceived(MatlabConnection sender, ErrorPacket errorPacket) throws MatlabException {
//		if (errorPacket.getErrorMessage() != null){
//			MeMoPlugin.err.println("error packet received " +  errorPacket.getErrorMessage());
//		}
//	}
//
//	@Override
//	public void responsePacketReceived(MatlabConnection arg0,
//			ResponsePacket packet) {
//		response = packet.getResponse();
//		if(response == null){
//			response = "null";
//		}
//		waitingForResponse = false;
//		gotResponse = true;
//
//	}
//
//	public boolean getParameterAsynchron(String object, String parameter) throws MatlabException{
//		if(!waitingForResponse){
//			waitingForResponse = true;
//			gotResponse = false;
//			this.object = object;
//			this.parameter = parameter;
//			matlab.getSLAPI().getParamAsynchron(object, parameter);
//			return true;
//		}else{
//			return false;
//		}
//	}
//
//	public void getVariableAsynchron(String varName) throws MatlabException{
//		waitingForResponse = true;
//		gotResponse = false;
//		matlab.getMatlabAPI().getVariableAsynchron(varName);
//		object = "get_var";
//		parameter = varName;
//	}
//
//	public Object getResponse(){
//		if(gotResponse){
//			gotResponse = false;
//			return response;
//		}
//		return null;
//	}
//
//	public boolean gotResponse() {
//		return gotResponse;
//	}
// }
