import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Driver {

	RandomAccessFile f; 
	int clusterSize;
	int quantClusters;	
	
	public Driver(int clusterSize, int quantClusters) {
		
		try {
			f = new RandomAccessFile("disco.txt","rw");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		this.clusterSize = clusterSize;
		this.quantClusters = quantClusters;
		createDisk(); 
	}
	
	private void createDisk() {
		try {
		 	for(int i=0; i < (clusterSize * quantClusters); i++)
			   f.write(0); 
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public void writeCluster(byte[] clusterData, int clusterNumber) {
		try {
			f.seek(clusterNumber * clusterSize);
			f.write(clusterData);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public byte[] readCluster(int clusterNumber) {
		byte [] b = new byte[clusterSize];	
		try {
			f.seek(clusterNumber * clusterSize);
			f.read(b);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return b;
	}	
	
	public void closeDisk() {
		try {
			f.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
