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

import java.awt.GridLayout;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import org.snmp4j.smi.TimeTicks;


public class InterfaceUptimeGUI extends JPanel{

	
    protected static final String licenseText = "Copyright (C) 2013 Mark Ciecior\r\n\r\n" +

											    "This program is free software; you can redistribute it and/or modify\r\n" +
											    "it under the terms of the GNU General Public License as published by\r\n" +
											    "the Free Software Foundation; either version 2 of the License, or\r\n" +
											    "(at your option) any later version.\r\n\r\n" +
											    
											    "This program is distributed in the hope that it will be useful,\r\n" +
											    "but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n" +
											    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n" +
											    "GNU General Public License for more details.\r\n\r\n" +
											
											    "You should have received a copy of the GNU General Public License along\r\n" +
											    "with this program; if not, write to the Free Software Foundation, Inc.,\r\n" +
											    "51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.\r\n\r\n" +
											    
											    "http://www.gnu.org/licenses/gpl.html\r\n\r\n";
    
    protected static final String aboutText = "Mark's Interface Uptime Finder\r\n" +
    										  "Version 1.1 (19 December 2013)\r\n" +
    										  "by Mark Ciecior, CCIE #28274\r\n" +
    										  "www.github.com/markciecior/InterfaceUptime";
    
    protected static final String helpText = "1) Enter the address/hostname and SNMP v2c community string of the\r\n" +
    										 "  access switch whose interfaces you want to scan.\r\n" +
    										 "2) Click the GO! button.\r\n" +
    										 "3) View the output in the center table.\r\n" +
    										 "4) Filter for the interface in which you're interested using the\r\n" +
    										 "  'Filter Interfaces' field on the bottom.\r\n" +
    										 "5) Enter a new address/hostname/community string and start again!\r\n";

	
	private static final long serialVersionUID = 1L;
	private static boolean TESTING = false;
	
	static JFrame frame;
	
	JTextField switchText;
	JTextField snmpText;
	JLabel statusLabel;
	
	HashMap<String,TimeTicks> IFNAME_TO_CHANGETIME = new HashMap<String, TimeTicks>();
	HashMap<String,String> IFNAME_TO_INTSTATUS = new HashMap<String,String>();
	
	MyTableModel model;
	
    private JTable table;
    private JTextField filterText;
    private TableRowSorter<MyTableModel> sorter;
    
    private String ACCESS_ADDR;
    private String ACCESS_SNMP;
    
	
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                 //Turn off metal's use of bold fonts
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        createAndShowGUI();
            }
        });

	}
	
	private static void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("Mark's Interface Status Finder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Add the File/Help menu bar
        frame.setJMenuBar(createMenu());
        //Add content to the window.
        frame.add(new InterfaceUptimeGUI());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }

    private static JMenuBar createMenu() {
    	JMenuBar menuBar;
        JMenu fileMenu, helpMenu;
        JMenuItem fileExit, helpHowTo, helpLicense, helpAbout;
        
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        helpMenu = new JMenu("Help");
        fileExit = new JMenuItem("Exit");
        helpHowTo = new JMenuItem("How To");
        helpLicense = new JMenuItem("License");
        helpAbout = new JMenuItem("About");
        
        fileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        fileExit.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		System.exit(1);
        	}
        });
        
        helpHowTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
        helpHowTo.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JOptionPane.showMessageDialog(frame, helpText, "How to Use", JOptionPane.PLAIN_MESSAGE);
        	}
        });
        
        helpLicense.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
        helpLicense.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JOptionPane.showMessageDialog(frame, licenseText, "License", JOptionPane.PLAIN_MESSAGE);
        	}
        });
        
        helpAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
        helpAbout.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JOptionPane.showMessageDialog(frame, aboutText, "About Mark's Device Collector", JOptionPane.PLAIN_MESSAGE);
        	}
        });

        fileMenu.add(fileExit);
        helpMenu.add(helpHowTo);
        helpMenu.add(helpLicense);
        helpMenu.add(helpAbout);
        menuBar.setVisible(true);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        return menuBar;

    }

    private static final Pattern VALID_PATTERN = Pattern.compile("[0-9]+|[a-zA-Z]+");

    private List<String> parse(String toParse) {
    	//Use this to break the interface names into chunks of digits and letters
        List<String> chunks = new LinkedList<String>();
        Matcher matcher = VALID_PATTERN.matcher(toParse);
        while (matcher.find()) {
            chunks.add( matcher.group() );
        }
        return chunks;
    }
    
	public InterfaceUptimeGUI() {
		
		super();
		
		try {
    		ACCESS_ADDR = Config.getSetting("accessSwitchAddr");
    		if (ACCESS_ADDR == "null") { ACCESS_ADDR = ""; }
    		
    		ACCESS_SNMP = Config.getSetting("accessSwitchSNMP");
    		if (ACCESS_SNMP == "null") { ACCESS_SNMP = ""; }
    		    		
    	} catch (Exception e) {
    		
    	}
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(3,2));
        JLabel switchLabel = new JLabel("Switch IP/Hostname:");
        JLabel snmpLabel = new JLabel("SNMPv2c Community String:");
        switchText = new JTextField(ACCESS_ADDR);
        snmpText = new JTextField(ACCESS_SNMP);
        inputPanel.add(switchLabel);
        inputPanel.add(switchText);
        switchLabel.setLabelFor(switchText);
        inputPanel.add(snmpLabel);
        inputPanel.add(snmpText);
        snmpLabel.setLabelFor(snmpText);
        JButton startButton = new JButton("GO!");
        startButton.addActionListener(new StartButtonListener());
        inputPanel.add(startButton);
        statusLabel = new JLabel();
        inputPanel.add(statusLabel);
        add(inputPanel);
 
        //Create a table with a sorter.
        model = new MyTableModel();
        sorter = new TableRowSorter<MyTableModel>(model);
        table = new JTable(model);
        Comparator<String> comparator = new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				//Break up our interface name into chunks of letters and digits
				List<String> list1 = parse(o1);
				List<String> list2 = parse(o2);
				Integer int1 = new Integer(0);
				Integer int2 = new Integer(0);
				String str1 = new String();
				String str2 = new String();
				
					//Go through each chunk and compare them
					//Try to compare number value if possible, otherwise compare String value
					//If both strings are interface names(Gix/y, Fax/y, Tex/y), treat them the same so Gi1/0/1 will be above Fa2/0/1
					for (int i=0; i < list1.size(); i++) {
						str1 = "";
						str2 = "";
						try {
							int1 = Integer.parseInt(list1.get(i));
						} catch (NumberFormatException f) {
							int1 = null;
							str1 = list1.get(i);
						} catch (ArrayIndexOutOfBoundsException a) {
							return -1;
						}
						try {
							int2 = Integer.parseInt(list2.get(i));
						} catch (NumberFormatException f) {
							int2 = null;
							str2 = list2.get(i);
						} catch (ArrayIndexOutOfBoundsException a) {
							return 1;
						}
						if (int1 == null && int2 == null){
							String pre1 = str1.substring(0,2);
							String pre2 = str2.substring(0,2);
							if ( (pre1.equalsIgnoreCase("Gi") || pre1.equalsIgnoreCase("Fa") || pre1.equalsIgnoreCase("Te")) &&
								 (pre2.equalsIgnoreCase("Gi") || pre2.equalsIgnoreCase("Fa") || pre2.equalsIgnoreCase("Te"))) {
									 continue;
								 }
							if (str1.compareTo(str2) == 0) {
								continue;
							} else { return str1.compareTo(str2); }
						} else if (int1 == null && int2 != null) {
							return -1;
						} else if (int1 != null && int2 == null) {
							return 1;
						} else {
							if (int1.compareTo(int2) == 0) {
								continue;
							} else { return int1.compareTo(int2); }
						}
					}
					return 0;
			}
        	
        };
        Comparator<TimeTicks> comparatorTime = new Comparator<TimeTicks>() {
        	//Use the default compareTo method in the TimeTicks class
			@Override
			public int compare(TimeTicks o1, TimeTicks o2) {
				return o1.compareTo(o2);
			}
        };
        sorter.setComparator(0, comparator);
        sorter.setComparator(2, comparatorTime);
        table.setRowSorter(sorter);
        table.getRowSorter().toggleSortOrder(0);
        table.setPreferredScrollableViewportSize(new Dimension(500, 400));
        table.setFillsViewportHeight(true);
 
        //For the purposes of this example, better to have a single
        //selection.
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  
        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
 
        //Add the scroll pane to this panel.
        add(scrollPane);
 
        //Create a separate form for filterText and statusText
        JPanel form = new JPanel(new GridLayout(1,2));
        JLabel l1 = new JLabel("Filter Interfaces:");//, SwingConstants.TRAILING);
        form.add(l1);
        filterText = new JTextField();
        //Whenever filterText changes, invoke newFilter.
        filterText.getDocument().addDocumentListener(
                new DocumentListener() {
                    public void changedUpdate(DocumentEvent e) {
                        newFilter();
                    }
                    public void insertUpdate(DocumentEvent e) {
                        newFilter();
                    }
                    public void removeUpdate(DocumentEvent e) {
                        newFilter();
                    }
                });
        l1.setLabelFor(filterText);
        form.add(filterText);
        add(form);
	}
	
	/** 
     * Update the row filter regular expression from the expression in
     * the text box.
     */
    private void newFilter() {
        RowFilter<MyTableModel, Object> rf = null;
        //If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter(filterText.getText(), 0);
        } catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }
	
	@SuppressWarnings("rawtypes")
	public static void printHashMap(HashMap myMap, HashMap myMap2){
    	Iterator iter = myMap.entrySet().iterator();
    	
    	while (iter.hasNext()){
    		Map.Entry pairs = (Map.Entry)iter.next();
    		String myKey = (String) pairs.getKey();
    		Object myValue = pairs.getValue();
    		Object myValue2 = myMap2.get(myKey);
    		System.out.println(myKey + " : " + myValue + " : " + myValue2);
    	}
    	
    }
	
	@SuppressWarnings("rawtypes")
	public void save(HashMap savedMap, String path) throws NotSerializableException{
    	try {
    		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
    		oos.writeObject(savedMap);
    		oos.flush();
    		oos.close();
    	} catch (Exception e){
    		e.printStackTrace();
    	}
    }
	
	@SuppressWarnings("rawtypes")
	public HashMap open(String path){
    	HashMap table = new HashMap();
    	
    	try {
    		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
    		table = (HashMap)ois.readObject();
    		ois.close();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	
    	return table;
    }
	
	class StartButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			
			Config.setSetting("accessSwitchAddr", switchText.getText());
        	Config.setSetting("accessSwitchSNMP", snmpText.getText());
        	
        	statusLabel.setText("Scanning " + switchText.getText() + "...");
        		
			InfoWorker myWorker = new InfoWorker();
			myWorker.execute();
			
		}
	}
    
	class InfoWorker extends SwingWorker<Void, Object[][]> {

		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		protected Void doInBackground() throws Exception {
			
			if (!TESTING){

				final SNMPManager man = new SNMPManager();
				try {
					man.start();
				} catch (IOException f) {
					f.printStackTrace();
				}
				
				String addr = switchText.getText();
				String comm = snmpText.getText();
				Long uptime = man.getUptime(addr, comm);
				
				HashMap<String, String> IFINDEX_TO_IFNAME = new HashMap<String,String>();
				IFINDEX_TO_IFNAME = man.getIfIndexToIfName(addr, comm);
				
				HashMap<String,Long> IFINDEX_TO_CHANGETIME = new HashMap<String,Long>();
				IFINDEX_TO_CHANGETIME = man.getIfIndexToChangeTime(addr, comm);
				
				IFNAME_TO_CHANGETIME = new HashMap<String,TimeTicks>();
				IFNAME_TO_CHANGETIME = man.getChangeTimes(IFINDEX_TO_IFNAME, IFINDEX_TO_CHANGETIME, uptime);
				
				IFNAME_TO_INTSTATUS = new HashMap<String,String>();
				IFNAME_TO_INTSTATUS = man.getIfNameToIntStatus(IFINDEX_TO_IFNAME, addr, comm);
			
				/*try {
					save(IFNAME_TO_CHANGETIME, "/home/mark/Desktop/int/IFNAME_TO_CHANGETIME.txt");
					save(IFNAME_TO_INTSTATUS, "/home/mark/Desktop/int/IFNAME_TO_INTSTATUS.txt");
				} catch (NotSerializableException g) {
					g.printStackTrace();
				}*/
				
			} else {
				IFNAME_TO_CHANGETIME = open("/home/mark/Desktop/int/IFNAME_TO_CHANGETIME.txt");
				IFNAME_TO_INTSTATUS = open("/home/mark/Desktop/int/IFNAME_TO_INTSTATUS.txt");
			}
			
			Iterator<Entry<String, String>> iter = IFNAME_TO_INTSTATUS.entrySet().iterator();
			int size = IFNAME_TO_INTSTATUS.size();
			
			Object[][] myData = new Object[size][3];
			
	    	for (int i=0; i < IFNAME_TO_INTSTATUS.size(); i++){
	    		Map.Entry pairs = (Map.Entry)iter.next();
	    		String myKey = (String) pairs.getKey();
	    		Object myValue = pairs.getValue();
	    		Object myValue2 = IFNAME_TO_CHANGETIME.get(myKey);
	    		myData[i][0] = myKey;
	    		myData[i][1] = myValue;
	    		myData[i][2] = myValue2;
	    	}
	    	publish(myData);
			return null;
		}
		
		public void process(List <Object[][]> chunks) {
			model.updateData(chunks.get(0));
		}
		
		public void done() {
			statusLabel.setText("Scanning complete");
			filterText.requestFocusInWindow();
		}
	}
	
	class MyTableModel extends AbstractTableModel {
	
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		String[] columnNames = {"Interface", "Status", "Last Change"};
		Object[][] data;
		
		
	public MyTableModel(){
		
	}

	@Override
	public int getRowCount() {
		return IFNAME_TO_CHANGETIME.size();
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		return data[rowIndex][columnIndex];
	}
	public String getColumnName(int column){
		String retVal = "";
		switch (column) {
			case 0:
				retVal = "Interface";
				break;
			case 1:
				retVal = "Status";
				break;
			default:
				retVal = "Last Changed";
				break;
		}
		return retVal;
	}
	
	public void updateData(Object[][] newData) {
		data = newData;
		this.fireTableDataChanged();
	}
	
	

	
	}
}
