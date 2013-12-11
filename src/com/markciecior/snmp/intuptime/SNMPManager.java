/*Copyright (C) 2013 Mark Ciecior

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.markciecior.snmp.intuptime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.TimeTicks;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.PDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

public class SNMPManager {
	
	static Snmp snmp = null;
	private String IFNAME_OID = ".1.3.6.1.2.1.31.1.1.1.1";
	private String CHANGETIME_OID = ".1.3.6.1.2.1.2.2.1.9";
	private String STATUS_OID = ".1.3.6.1.2.1.2.2.1.8";
	private String UPTIME_OID = ".1.3.6.1.2.1.1.3.0";

	
	public SNMPManager() {

	}
	
	public HashMap<String,String> getIfIndexToIfName(String addr, String community){
		List<TreeEvent> myNames = getBulkTree(snmp, new OID(IFNAME_OID), addr, community);
		HashMap<String,String> IFINDEX_TO_IFNAME = new HashMap<String,String>();
		Iterator<TreeEvent> iter = myNames.iterator();
		VariableBinding[] bind;

		while (iter.hasNext()){
			bind = iter.next().getVariableBindings();
			for (int i=0; i < bind.length; i++){
				String ifIndex = bind[i].getOid().toString().substring(IFNAME_OID.length());
				String ifName = bind[i].getVariable().toString();
				IFINDEX_TO_IFNAME.put(ifIndex, ifName);		
			}
			
		}
		return IFINDEX_TO_IFNAME;
	}
	
	public HashMap<String,Long> getIfIndexToChangeTime(String addr, String community){
		List<TreeEvent> myTimes = getBulkTree(snmp, new OID(CHANGETIME_OID), addr, community);
		HashMap<String,Long> IFINDEX_TO_CHANGETIME = new HashMap<String,Long>();
		Iterator<TreeEvent> iter = myTimes.iterator();
		VariableBinding[] bind;

		while (iter.hasNext()){
			bind = iter.next().getVariableBindings();
			for (int i=0; i < bind.length; i++){
				String ifIndex = bind[i].getOid().toString().substring(CHANGETIME_OID.length());
				Long changeTime = new Long(bind[i].getVariable().toLong());
				IFINDEX_TO_CHANGETIME.put(ifIndex, changeTime);		
			}
			
		}
		return IFINDEX_TO_CHANGETIME;
	}
	
	public HashMap<String,String> getIfNameToIntStatus(HashMap<String,String> ifIndextoIfName, String addr, String community){
		List<TreeEvent> myStatuses = getBulkTree(snmp, new OID(STATUS_OID), addr, community);
		HashMap<String,String> IFNAME_TO_IFSTATUS = new HashMap<String,String>();
		Iterator<TreeEvent> iter = myStatuses.iterator();
		VariableBinding[] bind;

		while (iter.hasNext()){
			bind = iter.next().getVariableBindings();
			for (int i=0; i < bind.length; i++){
				String ifIndex = bind[i].getOid().toString().substring(STATUS_OID.length());
				String ifStatus = bind[i].getVariable().toString();
				switch (ifStatus) {
				case "1":
						ifStatus = "Up";
						break;
				case "2":
						ifStatus = "Down";
						break;
				default:
						ifStatus = "?";
						break;
				}
				String ifName = ifIndextoIfName.get(ifIndex);
				IFNAME_TO_IFSTATUS.put(ifName, ifStatus);		
			}
			
		}
		return IFNAME_TO_IFSTATUS;
	}
	
	public Long getUptime(String addr, String community){
		//List<TreeEvent> myUptime = getBulkTree(snmp, new OID(UPTIME_OID), addr, community);
		Long myUptime = (long) 0;
		try {
			myUptime = getAsLong(new OID(UPTIME_OID), addr, community);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return myUptime;
		
	}
	
	public HashMap<String,TimeTicks> getChangeTimes(HashMap<String,String> ifIndextoIfName, HashMap<String,Long> ifIndextoChangeTime, Long uptime){
		Iterator<Map.Entry<String,String>> iter = ifIndextoIfName.entrySet().iterator();
		HashMap<String,TimeTicks> IFNAME_TO_CHANGETIME = new HashMap<String,TimeTicks>();
		
		while (iter.hasNext()){
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)iter.next();
			String myIndex = (String)pairs.getKey();
			String myIfName = ifIndextoIfName.get(myIndex);
			Long myChangeTime = ifIndextoChangeTime.get(myIndex);
			Long diff = uptime - myChangeTime;
			
			IFNAME_TO_CHANGETIME.put(myIfName, new TimeTicks(diff));
		}
		return IFNAME_TO_CHANGETIME;
	}

	
	/**
	* Start the Snmp session. If you forget the listen() method you will not
	* get any answers because the communication is asynchronous
	* and the listen() method listens for answers.
	* @throws IOException
	*/
	public void start() throws IOException {
		TransportMapping<UdpAddress> transport = new DefaultUdpTransportMapping();
		snmp = new Snmp(transport);
		// Do not forget this line!
		transport.listen();
	}

	/*This method actually performs the SNMPWalk operation and returns a list of VariableBinding arrays.
		By default the walk operation returns at most 10 OIDs in each array
	*/
	public List<TreeEvent> getBulkTree(Snmp mySnmp, OID myOid, String myAddress, String community) {
		Address address = GenericAddress.parse("udp:" + myAddress + "/161");
		PDUFactory factory = new DefaultPDUFactory(PDU.GETBULK);
		TreeUtils tree  = new TreeUtils(mySnmp, factory);
		return tree.getSubtree(getTarget(address, community), myOid);
		
	}

	/**
	* Method which takes a single OID and returns the response from the agent as a String.
	* @param oid
	* @return
	* @throws IOException
	*/
	
	public Long getAsLong(OID oid, String addr, String community) throws IOException {
		ResponseEvent event = get(new OID[] { oid }, addr, community);
		return new Long(event.getResponse().get(0).getVariable().toLong());
	}

	/**
	* This method is capable of handling multiple OIDs
	* @param oids
	* @return
	* @throws IOException
	*/
	
	public ResponseEvent get(OID oids[], String addr, String community) throws IOException {
		PDU pdu = new PDU();
		for (OID oid : oids) {
			pdu.add(new VariableBinding(oid));
			pdu.setMaxRepetitions(10);
		}
		pdu.setType(PDU.GET);
		Address address = GenericAddress.parse("udp:" + addr + "/161");
		ResponseEvent event = snmp.get(pdu, getTarget(address, community));
		//ResponseEvent event = snmp.send(pdu, getTarget(), null);
		if(event != null) {
			return event;
		}
		throw new RuntimeException("GET timed out");
	}
	
	/**
	* This method returns a Target, which contains information about
	* where the data should be fetched and how.
	* @return
	*/
	private Target getTarget(Address address, String community) {
		//Address targetAddress = GenericAddress.parse(address);
		//Address targetAddress = address;
		CommunityTarget target = new CommunityTarget();
		target.setCommunity(new OctetString(community));
		//target.setCommunity(community);
		target.setAddress(address);
		target.setRetries(4);
		target.setTimeout(5000);
		target.setVersion(SnmpConstants.version2c);
		return target;
	}

}


