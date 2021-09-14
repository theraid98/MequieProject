package catalogs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import utilities.Group;
import utilities.User;

public class GroupCatalog {

	private Map <Integer, Group> groups;
	private File groupFile;
	private static GroupCatalog INSTANCE;

	public static GroupCatalog getInstance() {
		if (INSTANCE == null)
			INSTANCE = new GroupCatalog();
		return INSTANCE;
	}

	private GroupCatalog() {
		groups = new ConcurrentHashMap<>();
		groupFile = new File("groupList.txt");
		readFile();
	}
	
	
	/**
	 * Id's of all the Groups of the Server
	 * @return - Keys with the id's of the Groups
	 */
	public Set<Integer> getGroups(){
		return this.groups.keySet();
	}

	/**
	 * Verifies if a Group already exists in the Server
	 * @param id - Id from the Group
	 * @return - true if the Group belongs to the Server
	 */
	public boolean containsGroup(int id) {
		return groups.get(id) != null;
	}

	/**
	 * Get a Group by the id
	 * @param id - Id of the Group wanted
	 * @return - Group wanted
	 */
	public Group getGroup(int id) {
		return this.groups.get(id);
	}

	/**
	 * Add a new Group to the Server
	 * @param info - Information of the Group 
	 * @throws IOException 
	 */
	public void addGroup(String info) throws IOException {
		Group g = Group.fromString(info);
		if (groups.containsKey(g.getID()))
			throw new IllegalArgumentException("-> Grupo ja existe.");
		groups.put(g.getID(), g);
		refreshFile(g);
	}
	
	/**
	 * Add a new Group to the Server
	 * @param id - Id of the Group
	 * @param currentUser - Owner of the Group
	 * @param adminKey 
	 * @throws IOException
	 */
	public void addGroup(int id, User currentUser) throws IOException {
		if (groups.containsKey(id))
			throw new IllegalArgumentException("-> Grupo ja existe.");
		Group g = new Group(id, currentUser);
		groups.put(id, g);
		refreshFile(g);
	}

	/**
	 * Total number of Groups in the Server
	 * @return - int with the number of Groups
	 */
	public int numberOfGroups() {
		return groups.size();
	}

	/**
	 * Read from the Group File
	 */
	private void readFile() {
		try {
			if (groupFile.createNewFile()) {
				// In this case, The file did not exist.
				System.out.println("-> Ficheiro de Grupo criado. Nenhum Grupo existe atualmente.");
				return;
			}
			System.out.println("-> Ficheiro de Grupo jah existe. A importar informacao.");
			BufferedReader in = new BufferedReader(new FileReader(groupFile));
			String line;
			while ((line = in.readLine()) != null) {
				String[] groupData = line.split(":");
				Group toAdd = Group.fromString(groupData[1]);
				groups.put(toAdd.getID(), toAdd); // group data will be added to file on its creation
			}
			in.close();

		} catch (IOException e) {
			System.out.println("Could not read Group information from file.");
			e.printStackTrace();
		}
	}

	/**
	 * To add a new Group to the Group Master file.
	 * @param g - Group to be added
	 * @throws IOException
	 */
	private void refreshFile(Group g) throws IOException {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(groupFile, true));
			out.write(g.toString());
			out.newLine();
			out.close();
		} catch (FileNotFoundException e) {
			System.out.println("Could not find GroupMasterFile.");
			e.printStackTrace();
		}
	}

	public void setKey(int groupID, byte[] adminKey) {
		this.groups.get(groupID).setAdminKey(adminKey);
		List<byte[]> x = new ArrayList<byte[]>();
		x.add(adminKey);
		this.groups.get(groupID).setKey(x);
	}
}
