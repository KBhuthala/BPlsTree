import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

// Parent class 
// Key can be of type double/float and Value can be of type String 
public class treesearch<Key extends Comparable<? super Key>, Value> {

	// root can be a leaf node or index node but never Null
	private Node root;
	// Maximum keySize (Number of Keys) in the leaf node
	private final int M;
	// Maximum keySize (Number of Keys) in the index node
	private final int N;

	// Creates a new B+ tree of order m
	public treesearch(int order) {
		this(order, order);
	}

	// B+ tree with different values of M and N
	public treesearch(int leafKeySize, int indexKeySize) {
		M = leafKeySize;
		N = indexKeySize;
		root = new LeafNode();
	}

	// Inserts (key, value) pair into the tree (leaf node)
	public void insert(Key key, Value value) {
		Split splitResult = root.insert(key, value);
		if (splitResult != null) {
			// Existing root is split into two parts, hence a new root is created pointing
			// to the two nodes
			IndexNode newRoot = new IndexNode();
			newRoot.keySize = 1;
			newRoot.keyArray[0] = splitResult.key;
			newRoot.children[0] = splitResult.leftNode;
			newRoot.children[1] = splitResult.rightNode;
			root = newRoot;
		}
	}

	// Searches for the provided key
	// If found, returns the list that contains all the values associated with the
	// key
	// If not found, returns an empty list
	public ArrayList<String> find(Key key) {

		Node node = root;
		ArrayList<String> list = new ArrayList<String>();
		// Traversing to the desired leaf node
		while (node instanceof treesearch.IndexNode) {
			@SuppressWarnings("unchecked")
			IndexNode inner = (IndexNode) node;
			int locIndex = inner.findLocation(key);
			node = inner.children[locIndex];
		}

		@SuppressWarnings("unchecked")
		/*
		 * Leaf nodes are connected in a doubly linked list. Now, Traversing through the
		 * leaf nodes for the given key using previous and next pointers. If the key in
		 * the leaf node matches with the search key, it is added to the list.
		 */

		LeafNode leafCurrent = (LeafNode) node;
		LeafNode leafPrevious = leafCurrent.prev;
		while (leafPrevious != null && leafPrevious.findLocation(key) != leafPrevious.keySize) {
			for (int i = leafPrevious.findLocation(key); i < leafPrevious.keySize; i++) {
				if (leafPrevious.keyArray[i].equals(key))
					list.add((String) leafPrevious.valueArray[i]);
			}
			leafPrevious = leafPrevious.prev;
		}
		while (leafCurrent != null && leafCurrent.findLocation(key) != leafCurrent.keySize) {
			for (int i = leafCurrent.findLocation(key); i < leafCurrent.keySize; i++) {
				if (leafCurrent.keyArray[i].equals(key))
					list.add((String) leafCurrent.valueArray[i]);
			}
			leafCurrent = leafCurrent.next;
		}
		return list;
	}

	/*
	 * Search operation is performed within a Range(leftMostKey and rightMostKey).
	 * If found, the list with all the (key,value) pairs within the
	 * Range is returned. If not found, an empty list is returned
	 */
	public ArrayList<String> findInRange(Key leftMostKey, Key rightMostKey) {
		Node node = root;
		ArrayList<String> list = new ArrayList<String>();
		// Traversing to the desired leaf node
		while (node instanceof treesearch.IndexNode) {
			@SuppressWarnings("unchecked")
			IndexNode inner = (IndexNode) node;
			int locIndex = inner.findLocation(leftMostKey);
			node = inner.children[locIndex];
		}

		@SuppressWarnings("unchecked")
		/*
		 * Leaf nodes are connected in a doubly linked list. Now, Traversing through the
		 * leaf nodes within the leftmost and rightmost key using previous and next
		 * pointers. If the key in the leaf node is within the search range, it is added
		 * to the list.
		 */
		LeafNode leafCurrent = (LeafNode) node;
		LeafNode leafPrevious = leafCurrent.prev;
		while (leafPrevious != null && leafPrevious.findLocation(leftMostKey) != leafPrevious.keySize) {
			for (int i = leafPrevious.findLocation(leftMostKey); i < leafPrevious.keySize; i++) {
				if (leafPrevious.keyArray[i].compareTo(leftMostKey) >= 0) {
					list.add("(" + leafPrevious.keyArray[i] + "," + leafPrevious.valueArray[i] + ")");
				}
			}
			leafPrevious = leafPrevious.prev;
		}
		while (leafCurrent != null) {
			for (int i = leafCurrent.findLocation(leftMostKey); i < leafCurrent.keySize; i++) {
				if (leafCurrent.keyArray[i].compareTo(leftMostKey) >= 0
						&& leafCurrent.keyArray[i].compareTo(rightMostKey) <= 0) {
					list.add("(" + leafCurrent.keyArray[i] + "," + leafCurrent.valueArray[i] + ")");
				}
			}
			leafCurrent = leafCurrent.next;
		}
		return list;
	}

	// Node class - used by the leafNode and indexNode classes
	abstract class Node {
		// keySize represents the number of Keys
		protected int keySize;

		// keyArray has all the keys stored within it
		protected Key[] keyArray;

		abstract public int findLocation(Key key);

		abstract public Split insert(Key key, Value value);
	}

	// Split class that has the variables the key, left and right nodes
	class Split {
		public final Key key;
		public final Node leftNode;
		public final Node rightNode;

		public Split(Key key, Node leftNode, Node rightNode) {
			this.key = key;
			this.leftNode = leftNode;
			this.rightNode = rightNode;
		}
	}

	@SuppressWarnings("unchecked")
	// Index Node Class. Subclass of Node Class. Index nodes are not connected
	class IndexNode extends Node {
		final Node[] children = new treesearch.Node[N];
		{
			keyArray = (Key[]) new Comparable[N - 1];
		}

		// find the position of the given key among the Index nodes
		public int findLocation(Key key) {
			// Compares the existing keys of the index nodes with the given key
			for (int i = 0; i < keySize; i++) {
				if (keyArray[i].compareTo(key) > 0) {
					return i;
				}
			}
			return keySize;
		}

		/*
		 * Used to check if split occurs when a new key is inserted, if the existing
		 * index node is split, returns the split information. if the existing index
		 * node is not split, returns null
		 */
		public Split insert(Key key, Value value) {

			if (this.keySize == N - 1) { // Split occur
				int mid = (N) / 2;
				int siblingSize = this.keySize - mid;
				IndexNode sibling = new IndexNode();
				sibling.keySize = siblingSize;
				System.arraycopy(this.keyArray, mid, sibling.keyArray, 0, siblingSize);
				System.arraycopy(this.children, mid, sibling.children, 0, siblingSize + 1);
				this.keySize = mid - 1; // The middle key is set to reach the next height, without the repetition of the
										// index node key
				// As split occurs, the return variable is computed
				Split splitResult = new Split(this.keyArray[mid - 1], this, sibling);
				// Insertion occurs at the relevant sibling
				if (key.compareTo(splitResult.key) < 0) {
					this.noSplitInsert(key, value);
				} else {
					sibling.noSplitInsert(key, value);
				}
				return splitResult;
			} else { // Split does not occur as the Node is not full. Hence, noSplitInsert is done
				this.noSplitInsert(key, value);
				return null;
			}
		}

		// Inserts into a index node which is not full (No Split occurs)
		private void noSplitInsert(Key key, Value value) {
			// find the position of key
			int locIndex = findLocation(key);
			Split result = children[locIndex].insert(key, value);

			if (result != null) {
				if (locIndex == keySize) {
					// Insertion occurs at the rightmost key
					keyArray[locIndex] = result.key;
					children[locIndex] = result.leftNode;
					children[locIndex + 1] = result.rightNode;
					keySize++;
				} else {
					/*
					 * Insertion is not at the rightmost Key. Shifts index > locIndex to the right
					 */
					System.arraycopy(keyArray, locIndex, keyArray, locIndex + 1, keySize - locIndex);
					System.arraycopy(children, locIndex, children, locIndex + 1, keySize - locIndex + 1);
					children[locIndex] = result.leftNode;
					children[locIndex + 1] = result.rightNode;
					keyArray[locIndex] = result.key;
					keySize++;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	// leaf Node Class which has all the leaf nodes connected in the doubly linked list
	// Subclass of Node Class
	class LeafNode extends Node {

		// prev and next are initialized and are used to implement the doubly linked
		// list for Leaf nodes
		@SuppressWarnings("unused")
		private LeafNode prev = null;
		private LeafNode next = null;
		final Value[] valueArray = (Value[]) new Object[M];
		{
			keyArray = (Key[]) new Comparable[M - 1];
		}

		// finds the position of the given key among the Leaf nodes
		public int findLocation(Key key) {
			// compares the existing keys of leaf nodes with given key
			for (int i = 0; i < keySize; i++) {
				if (keyArray[i].compareTo(key) >= 0) {
					return i;
				}
			}
			return keySize;
		}

		/*
		 * Used to check if split occurs when a new key is inserted, if the existing
		 * leaf node is split, returns the split information. if the existing leaf node
		 * is not split, returns null
		 */
		public Split insert(Key key, Value value) {
			int i = findLocation(key);
			// leaf node is full, hence split is performed
			if (this.keySize == M - 1) {
				int mid = (M) / 2;
				int siblingSize = this.keySize - mid;
				LeafNode sibling = new LeafNode();
				// When a new node is inserted, the prev and next pointers are changed
				// accordingly
				sibling.prev = this;
				if (this.next != null) {
					sibling.next = this.next;
					sibling.next.prev = sibling;
				}
				this.next = sibling;
				sibling.keySize = siblingSize;
				System.arraycopy(this.keyArray, mid, sibling.keyArray, 0, siblingSize);
				System.arraycopy(this.valueArray, mid, sibling.valueArray, 0, siblingSize);
				this.keySize = mid;
				if (i < mid) {
					// Insert into left sibling
					this.noSplitInsert(key, value, i);
				} else {
					// Insert into right sibling
					sibling.noSplitInsert(key, value, i - mid);
				}

				// Split information is returned
				Split splitResult = new Split(sibling.keyArray[0], this, sibling);
				return splitResult;
			} else { // Split does not occur as the Node is not full. Hence, noSplitInsert is done
				this.noSplitInsert(key, value, i);
				return null;
			}
		}

		// Inserts into a leaf node which is not full (No Split occurs)
		private void noSplitInsert(Key key, Value value, int locIndex) {
			// Allows duplicate values
			System.arraycopy(keyArray, locIndex, keyArray, locIndex + 1, keySize - locIndex);
			System.arraycopy(valueArray, locIndex, valueArray, locIndex + 1, keySize - locIndex);
			keyArray[locIndex] = key;
			valueArray[locIndex] = value;
			keySize++;
		}
	}

	// Main method that calls all the implemented methods
	public static void main(String[] args) {
		// Initialising input file
		File inputFile = null;
		if (0 < args.length) {
			inputFile = new File(args[0]);
		}
		ArrayList<String> inputInstList = new ArrayList<String>();
		BufferedReader bfr = null;
		try {
			String lineOfInst;
			bfr = new BufferedReader(new FileReader(inputFile));
			// Read the instructions in the input file line by line and adds them to a list
			while ((lineOfInst = bfr.readLine()) != null) {
				inputInstList.add(lineOfInst);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			// Initialize the output file
			PrintWriter outputFile = new PrintWriter("output_file.txt");
			String currentInst = null;
			String category;
			// Reading the order specified in the input file
			for (int i = 0; i < 1; i++) {
				currentInst = inputInstList.get(i);
			}
			int order = Integer.parseInt(currentInst);
			// initializing a B+ tree with the specified order. Key type is double and Value
			// type is String
			treesearch<Double, String> tree = new treesearch<Double, String>(order);
			String[] parts;
			String pair, first, second, value;
			Double key, key1, key2;

			// Executing all the instructions one by one
			for (int i = 1; i < inputInstList.size(); i++) {
				currentInst = inputInstList.get(i);
				// Store first 6 characters of the instruction into category
				category = currentInst.substring(0, 6);
				// If category is Insert, Insert method is called
				if (category.equalsIgnoreCase("Insert")) {
					pair = currentInst.substring(7, currentInst.length() - 1);
					parts = pair.split(",");
					first = parts[0];
					second = parts[1];
					key = Double.parseDouble(first);
					value = second;
					tree.insert(key, value);
				} else {
					// If category is Search
					ArrayList<String> outList = new ArrayList<String>();
					pair = currentInst.substring(7, currentInst.length() - 1);
					// Checking if its a normal search or range search
					if (pair.contains(",")) {
						// Range Search method is called
						parts = pair.split(",");
						first = parts[0];
						second = parts[1];
						key1 = Double.parseDouble(first);
						key2 = Double.parseDouble(second);
						outList = tree.findInRange(key1, key2);
					} else {
						// Search method is called
						key = Double.parseDouble(pair);
						outList = tree.find(key);
					}
					String out = null;
					if (outList.size() == 0) {
						/*
						 * Writes Null to the output file if the key is not found in the tree or if
						 * there are no keys within the specified range
						 */
						outputFile.println("Null");
					} else {
						for (int j = 0; j < outList.size(); j++) {
							if (outList.size() == 1) {
								out = outList.get(j);
							} else {
								if (out != null) {
									out = out + outList.get(j);
								} else {
									out = outList.get(j);
								}
								if (j != outList.size() - 1) {
									out = out + ", ";
								}
							}
						}
						// Write to the output file
						outputFile.println(out);
					}
				}
			}
			outputFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

	}

}
