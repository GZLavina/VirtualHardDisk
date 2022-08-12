import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.ArrayList;

public class DiscoVirtual {

	Driver d;
	Teclado t;

	int clusterSize = 240;    //240 bytes
	int quantClusters = 2160; //2160 clusters
	int bufferSize = clusterSize * 20;
	
	byte[] buffer;
	int[] vetFAT;
	ArrayList<DirEntry> dirAtual;
	int dirFAT;
	String[] dirPath;
	
	public DiscoVirtual() {

		this.d = new Driver(clusterSize, quantClusters);	
		this.t = new Teclado();
		this.buffer = new byte[bufferSize]; //buffer com espaço para 20 clusters
		this.vetFAT = new int[quantClusters];
		this.dirAtual = new ArrayList<>();
		this.dirFAT = 20; // raiz
		String[] path = new String[1];
		path[0] = "disco";
		this.dirPath = path;
		
	}
	
	public String getNomeAtual() {
		return this.dirPath[this.dirPath.length - 1];
	}
	
	public String getPath() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < dirPath.length; i++) {
			if (i == dirPath.length - 1)
				sb.append(dirPath[i]);
			else
				sb.append(dirPath[i] + "/");
		}
		
		return sb.toString();
	}
	
	public void inicializaFAT() {
		
		//seta valores iniciais na vetFAT
		vetFAT[0] = 0; //cluster de boot
		for(int i=1; i < 19; i++) {  
			vetFAT[i] = i+1;   //encadeamento dos clusters da FAT
		}
		vetFAT[19] = 0; //ultimo cluster da FAT
		vetFAT[20] = 0; //cluster do diretorio raiz
		
		for(int i=21; i < 2160; i++) {  
			vetFAT[i] = 9999;  //clusters livres
		}
		//copia vetFAT para disco (clusters)
		atualizaFATnoDisco();
	}
	
	private Queue<Integer> arrayClustersFAT() {
		Queue<Integer> array = new ArrayDeque<Integer>(19);
		
		for(int i = 0; i < 19; i++) {
			array.add(i);
		}
		
		return array;
	}
	
	private void atualizaFATnoDisco() {
		//copia vetFAT para buffer
		int j = 0;
		for(int i=0; i < quantClusters;i++) {
				buffer[j] = (byte) (vetFAT[i]/256);
				buffer[j+1] = (byte) (vetFAT[i] - buffer[j]*256);
				j+=2;
		}
		//grava buffer no disco
		writeBufferOnDisk(arrayClustersFAT(), quantClusters*2); //grava FAT usando os indices no array
	}
	
	/*
	 * writeBufferOnDisk recebe o numero do clusterInicial e quantos
	 * clusters serão gravados a partir deste cluster 
	 */
	public void writeBufferOnDisk(Queue<Integer> indexes, int totalBytes) {
//		System.out.println("size = " + indexes.size());
//		System.out.println("Tamanho = " + totalBytes);
		byte[] cluster = new byte[clusterSize];
		int countBytes = 0;
		
		for (int i = 0; i < getTeto(totalBytes); i++) {
			//para cada cluster, grava clusterSize bytes...
			for(int j = 0; j < clusterSize; j++) {
	    		countBytes++;
	    		if (countBytes > totalBytes) {
	    			// completa cluster com zeros depois de terminar o arquivo
	    			cluster[j] = 0;
	    			continue;
	    		}
	    		cluster[j] = buffer[i*clusterSize+j];
	    	}  
	    	
	    	d.writeCluster(cluster, indexes.poll());
		}
	}
	
	public void writeClusterOnDisk(int index, int totalBytes) {   // metodo para escrever apenas um cluster, nao sei se eh necessario
		byte[] cluster = new byte[clusterSize];

    	for(int j = 0; j < totalBytes; j++) {
    		cluster[j] = buffer[j];
    	}  
    	
    	d.writeCluster(cluster, index);
	}
	
	public Queue<Integer> procuraLivres(int tam) {
		int count = 0; 
		int valor;
		Queue<Integer> indexes = new ArrayDeque<Integer>(tam);
		
		for(int i = 21; i < this.vetFAT.length; i++) {
			 
			valor = this.vetFAT[i];
			if(valor == 9999) {
				
				indexes.add(i);
				count++;
				
				if(count >= tam) {
					return indexes;
				}
			}
		}
		return null;
	}
	
	// procura um unico cluster e ja aloca
	public int procuraLivre() {
		for (int i = 21; i < quantClusters; i++) {
			if (this.vetFAT[i] == 9999) {
				this.vetFAT[i] = 0;
				atualizaFATnoDisco();
				return i;
			}
		}
		
		return -1;
	}
	
	public boolean temEspaco(int tam) {
		int count = 0;
		for (int i = 21; i < quantClusters; i++) {
			if (this.vetFAT[i] == 9999)
				count++;
			
			if (count == tam) {
				return true;
			}
		}
		
		return false;
	}
	
	public void alocaNaFAT(Queue<Integer> indexes) {
		int size = indexes.size();
		Integer indexA;
		Integer indexB;
		for (int i = 0; i < size; i++) {
			indexA = indexes.poll();
			if ((indexB = indexes.peek()) != null)
				this.vetFAT[indexA] = indexB;
			else
				this.vetFAT[indexA] = 0;
		}
	}
	
	public void liberaNaFAT(int primeiro) {
		int aux;
		while (this.vetFAT[primeiro] != 0) {
			aux = this.vetFAT[primeiro];
			this.vetFAT[primeiro] = 9999;
			primeiro = aux;
		}
		
		this.vetFAT[primeiro] = 9999;
	}	
	
	public int getTeto(double tam) {
		int teto;
		
		teto = (int) Math.ceil(tam/clusterSize);	
		
		return teto;
	}
	
	/*
	 * Importa arquivo externo e copia para buffer
	 */
	public void importaArquivo(String nomeArquivo){
		
		int tam = 0;
		int count = 0;
		ArrayDeque<Integer> indexes;
		
		try {
			FileInputStream fis = new FileInputStream(nomeArquivo);
			
			byte byteLido;
			
			while((byteLido = (byte) fis.read()) != -1) {   //enquanto nao chegar no final do arquivo...
				count++;				
			}
			
			if((indexes = (ArrayDeque<Integer>) procuraLivres(getTeto(count))) != null) {
				if (jaExiste(nomeArquivo)) {
					System.out.print("Um outro elemento com este nome ja existe neste diretorio.");
					fis.close();
					return;
				}
				
				if (!insereDirEntry(nomeArquivo, 'A', indexes.peek(), count)) {      // tenta inserir no dirAtual
					System.out.println("Nao foi possivel gravar o arquivo no disco virtual!");
					fis.close();
					return;
				}
				alocaNaFAT(indexes.clone()); // Pega os indices livres e encadeia na FAT		
			} else {
				System.out.println("Nao ha espaco suficiente!");
				fis.close();
				return;
			}
			
			
			
			fis.close();
			fis = new FileInputStream(nomeArquivo);
			boolean encheu = false;
			while((byteLido = (byte) fis.read()) != -1) {   //enquanto nao chegar no final do arquivo...
				buffer[tam] = byteLido;
				if(tam == count - 1 || tam == bufferSize - 1) {
					writeBufferOnDisk(indexes, tam+1);
					tam = 0;
					encheu = true;
				} else {
					tam++;
					encheu = false;
				}
			}

			//insere o que sobrou (se tiver sobrado)
			if (!encheu) {
				writeBufferOnDisk(indexes, tam);
			}
			
			fis.close();
		} catch(IOException e){
			System.out.println("nao foi possivel ler o arquivo...");
		}
		
		atualizaFATnoDisco();
	}
	
	public boolean criaDiretorio(String nome) {
		int indexFAT = procuraLivre();
		
		// limpa o buffer que vai limpar disco no cluster IndexFAT
		for (int i = 0; i < clusterSize; i++) {
			buffer[i] = 0;
		}
		
		writeClusterOnDisk(indexFAT, clusterSize);
		
		if (insereDirEntry(nome, 'D', indexFAT, 0)) {
			return true;
		}
		
		System.out.println("return false");
		return false;
	}
	
	public int medeClusters(int index) {
		int countIndex = 1;
		while ((index = this.vetFAT[index]) != 0) {  
			countIndex++;
		}
		return countIndex;
	}
	
	public void carregaRaiz() {
		this.dirFAT = 20;
		int countIndex = medeClusters(20);
		
		String[] path = new String[1];
		path[0] = "disco";
		this.dirPath = path;
				
		montaDir(readDisk(this.dirFAT, countIndex), countIndex);
	}
	
	public boolean carregaSubpasta(int index) {
		
		DirEntry de;
		try {
			de = dirAtual.get(index);
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Indice invalido!");
			return false;
		}
		
		if (de.getTipo() != 'D') {
			System.out.println(de.getNomeComExt() + " nao eh uma pasta!");
			return false;
		}
	
		aumentaPath(de.getNome());
		
		this.dirFAT = de.getPrimeiroCluster();
		int tam = medeClusters(this.dirFAT);
		
		montaDir(readDisk(this.dirFAT, tam), tam);
		return true;
	}
	
	public boolean carregaSubpasta(String nome) {
		int tam = 0;
		boolean achou = false;
		for (int i = 0; i < this.dirAtual.size(); i++) {
			if (dirAtual.get(i).getNome().equalsIgnoreCase(nome) && dirAtual.get(i).getTipo() == 'D') {	
				achou = true;
				this.dirFAT = dirAtual.get(i).getPrimeiroCluster();
				tam = medeClusters(dirFAT);
				break;
			}
		}
		
		if (!achou) {
			System.out.println("Nenhum diretorio encontrado com esse nome.");
			return false;
		}
		
		aumentaPath(nome);
		
		montaDir(readDisk(this.dirFAT, tam), tam);
		return true;
	}
	
	public void aumentaPath(String nome) {
		String[] newPath = new String[this.dirPath.length + 1];
		for (int i = 0; i < this.dirPath.length; i++) {
			newPath[i] = this.dirPath[i];
		}
		newPath[this.dirPath.length] = nome;
		this.dirPath = newPath;
	}
	
	public boolean carregaPastaPai() {
		if (this.dirPath.length == 1) {
			System.out.println("Diretorio raiz nao possui pasta pai!");
			return false;
		}
		
		// faz um novo array path, mas sem o ultimo dir.
		String[] path = new String[this.dirPath.length-1];
		for (int i = 0; i < path.length; i++) {
			path[i] = this.dirPath[i];
		}
		
		carregaPathDaRaiz(path);
		
		return true;
	}
	
	
	// este é para alguns metodos recursivos.
	public boolean carregaPastaPaiRec() {
		if (this.dirPath.length == 1) {
			return false;
		}
		
		// faz um novo array path, mas sem o ultimo dir.
		String[] path = new String[this.dirPath.length-1];
		for (int i = 0; i < path.length; i++) {
			path[i] = this.dirPath[i];
		}
		
		carregaPathDaRaiz(path);
		
		return true;
	}
	
	public String[] pathToArray(String path) {
		String[] array;
		try {
			array = path.split("/");
			if (array.length == 0) {
				System.out.println("Path formatado incorretamente!");
				return null;
			}
			for (int i = 0; i < array.length; i++) {
				if (array[i].equals("")) {
					System.out.println("Path formatado incorretamente!");
					return null;
				}
			}
		} catch (Exception e) {
			System.out.println("Path formatado incorretamente!");
			return null;
		}
		
		return array;
	}
	
	public boolean carregaPathDaRaiz(String[] path) {
		carregaRaiz();
		for (int i = 1; i < path.length; i++) {
			if (!carregaSubpasta(path[i])) {
				//System.out.println("Path informado nao existe!");
				return false; // nao sendo possivel carregar algum diretorio no caminho, retorna falso.
			}
		}
		return true;
	}
	
	public boolean carregaPath(String pathStr) { // Pode retornar boolean
		String[] path;
		if ((path = pathToArray(pathStr)) == null)
			return false;
		
		if (carregaPath(path))
			return true;
		else
			return false;
	}
	
	public boolean carregaPath(String[] path) { // Pode retornar boolean
		if (pathEquals(path))
			return true;
		
		String[] pathAnterior = this.dirPath.clone();
		
		// verifica se o dirAtual representa algum dos diretorios no caminho (incluindo o raiz)
		r1:for (int i = 0; i < path.length; i++) {
			if (path[path.length-1-i].equalsIgnoreCase(this.dirPath[this.dirPath.length-1])) {
				r2:for (int j = 1; j < path.length-i ; j++) {
					// ve se o diretorio com o mesmo nome do dirAtual realmente eh o mesmo.
					if (path[path.length-1-i-j].equalsIgnoreCase(this.dirPath[this.dirPath.length-1-j])) {
						continue r2;
					} else {
						break r1;
					}
				}
				
				// se era o mesmo, vai pegando de pasta em pasta pelos nomes no array path.
				for (int j = 1; j <= i; j++) {
					// da pra fazer melhor que path[path.length-1-i+j] ??
					if (!carregaSubpasta(path[path.length-1-i+j])) {
						carregaPath(pathAnterior);
						return false;
					} 
				}
				return true;
			}
		}
		
		// Se nao foi possivel cortar caminho, entao vai da raiz.
		if (carregaPathDaRaiz(path))
			return true;
		else {
			carregaPath(pathAnterior);
			return false;
		}
	}
	
	public boolean pathEquals(String[] path) {
		if (path.length == this.dirPath.length) {
			for (int i = 0; i < path.length; i++) {
				if (!path[i].equals(this.dirPath[i])) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	public byte[] readDisk(int indexFAT, int qClusters) {
		byte[] dirBytes;
		if (qClusters == 1) {
			dirBytes = d.readCluster(indexFAT);
		} else {
			dirBytes = new byte[clusterSize*qClusters];
			byte[] temp;
			for (int i = 0; i < qClusters; i++) {
				
				// le o conteudo do indice indexFAT, qClusters vezes.
				temp = d.readCluster(indexFAT);
				for(int j = 0; j < clusterSize; j++) {
		    		dirBytes[i*clusterSize+j] = temp[j];
		    	}
				
				indexFAT = this.vetFAT[indexFAT];
			}
		}
		//System.out.println("leu");
		return dirBytes;
	}
	
	public void montaDir(byte[] dirBytes, int tam) {
		//System.out.println("tam = " + tam);
		ArrayList<DirEntry> newDir = new ArrayList<>();
		r1:for (int i = 0; i < tam*10; i++) {  // repete o processo 10 vezes para cada cluster.
			
			StringBuilder nome = new StringBuilder();
			char c;
			for (int j = 0; j < 16; j++) {              // insere um por um de 16 bytes como char no StringBuilder. Para no 0.
				if ((c = (char) dirBytes[i*24+j]) != 0) {
					nome.append(c);
				}                    
				else if (c == 0 && j == 0) {         
					break r1;   // Se o primeiro byte = 0, acabaram as entradas.
				}
				else {
					break;
				}
			}
			
			
			StringBuilder ext = new StringBuilder();     // faz a mesma coisa que a parte de cima, mas para a ext.
			for (int j = 16; j < 19; j++) {              // talvez stringBuilder seja um exagero.
				if ((c = (char) dirBytes[i*24+j]) != 0)
					ext.append(c);
				else 
					break;
			}
			
			char tipo = (char) dirBytes[i*24+19];   // pega o char do tipo
			
			int cluster = (dirBytes[i*24+20] & 0xff) * 256 + (dirBytes[i*24+21] & 0xff);
			int tamArq;
			if (tipo == 'A') {
				tamArq = (dirBytes[i*24+22] & 0xff) * 256 + (dirBytes[i*24+23] & 0xff); // & 0xff transforma o byte em unsigned (0 a 255 (int)).
			} else {
				tamArq = -1;
			}
			
			if (ext.toString().equals(""))
				newDir.add(new DirEntry(nome.toString(), null, tipo, cluster, tamArq));
			else
				newDir.add(new DirEntry(nome.toString(), ext.toString(), tipo, cluster, tamArq));
		}
		
		//System.out.println("montou");
		this.dirAtual = newDir;
	}
	
	public boolean move(int index, String[] path) {
		DirEntry de;
		try {
			
			de = dirAtual.get(index);
			if (de.getTipo() == 'D') {
				String nomeDir = de.getNome();
				for (int i = path.length-1; i > 0; i--) {
					if (path[i].equalsIgnoreCase(nomeDir) && path[i-1].equalsIgnoreCase(getNomeAtual())) {
						System.out.println("Nao eh permitido mover um diretorio para uma propria subpasta!");
						return false;
					}
				}
			}
			
			dirAtual.remove(index);
			gravaDirAtual();
			
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Indice invalido!");
			return false;
		}
		
		//gravaDirAtual();
		String[] pathAnterior = this.dirPath.clone();
		
		if (!carregaPath(path)) {
			System.out.println("Caminho invalido!");
			carregaPath(pathAnterior);
			dirAtual.add(index, de); // restaura e reinsere em caso de erro
			gravaDirAtual();
			return false;
		}
		
		if (jaExiste(de.getNomeComExt())){
			System.out.println("Um outro elemento (de mesmo tipo) com este nome ja existe no novo diretorio.");
			carregaPath(pathAnterior);
			dirAtual.add(index, de); // restaura e reinsere em caso de erro
			gravaDirAtual();
			return false;
		}
		
		dirAtual.add(de);
		gravaDirAtual();
		
		return true;
	}
	
	// separar em dois?? (insereDirEntryArq e insereDirEntryDir)
	public boolean insereDirEntry(String nome, char tipo, int cluster, int tam) {
		try {	
			
			if (dirAtual.size() >= 199) {
				System.out.println("Este diretorio nao pode receber mais entradas!");
				return false;
			}
			
			if (jaExiste(nome)) {
				System.out.print("Um outro elemento com este nome ja existe neste diretorio.");
				return false;
			}
			
			String[] info = nome.split("[.]");  // separa nome da ext no .
			boolean arquivo = true;
			if (tipo == 'A') {	
				arquivo = true; 
				// ve se algum tamanho esta incorreto
				if (info[0].length() > 16 || info[1].length() > 3 || info[1].length() == 0) // arquivo inserido deve possuir ext, se quisermos
					return false;                                                           // permitir que nao tenha ext, precisamos ver
			} else if (tipo == 'D') {                                                       // oq fazer quando dir e arq tiverem o mesmo nome
				arquivo = false; 
			} else {
				return false;  // Se nao era 'A' ou 'D', nao insere
			}
			
			
			
			// Se size() % 10 == 0, quer dizer que o arquivo esta cheio ou esta vazio
			if (dirAtual.size() % 10 != 0 || dirAtual.isEmpty()) {  
				if (arquivo) {
					dirAtual.add(new DirEntry(info[0], info[1], tipo, cluster, tam));
					gravaDirAtual();
					return true;
				} else {
					dirAtual.add(new DirEntry(nome, null, tipo, cluster, -1));  // gera DirEntry com null na ext para dir
					gravaDirAtual();                                            // nao sei se ter nome + ext no mesmo atributo era mais facil
					return true;
				}
			}
			
			// se conseguir aumentar (se tiver espaco), insere o arquivo.
			if (aumentaDirAtual()) {
				if (arquivo) {
					dirAtual.add(new DirEntry(info[0], info[1], tipo, cluster, tam));
					gravaDirAtual();
					return true;
				} else {
					dirAtual.add(new DirEntry(nome, null, tipo, cluster, -1));  
					gravaDirAtual();
					return true;
				}
			}
			
			return false;
			
		} catch(Exception e) {
			//e.printStackTrace();
			return false;
		}
		
	}
	
	public boolean insereCopia(String nome, char tipo, int cluster, int tam, ArrayList<DirEntry> dir) {
		try {
			
			if (dir.size() >= 199) {
				System.out.println("Este diretorio nao pode receber mais entradas!");
				return false;
			}
			
			if (jaExiste(nome, dir)) {
				System.out.println("Arquivo " + nome + " ja existia novo diretorio.");
				return false;
			}
			
			String[] info = nome.split("[.]");  // separa nome da ext no .
			boolean arquivo = true;
			if (tipo == 'A') {	
				arquivo = true; 
				// ve se algum tamanho esta incorreto
				if (info[0].length() > 16 || info[1].length() > 3 || info[1].length() == 0) // arquivo inserido deve possuir ext, se quisermos
					return false;                                                           // permitir que nao tenha ext, precisamos ver
			} else if (tipo == 'D') {                                                       // oq fazer quando dir e arq tiverem o mesmo nome
				arquivo = false; 
			} else {
				return false;  // Se nao era 'A' ou 'D', nao insere
			}
			
			
			
			// Se size() % 10 == 0, quer dizer que o arquivo esta cheio ou esta vazio
			if (dir.size() % 10 != 0 || dir.isEmpty()) {  
				if (arquivo) {
					dir.add(new DirEntry(info[0], info[1], tipo, cluster, tam));
					gravaDirAtual();
					return true;
				} else {
					dir.add(new DirEntry(nome, null, tipo, cluster, -1));  // gera DirEntry com null na ext para dir
					gravaDirAtual();                                            // nao sei se ter nome + ext no mesmo atributo era mais facil
					return true;
				}
			}
			
			// se conseguir aumentar (se tiver espaco), insere o arquivo.
			if (aumentaDirAtual()) {
				if (arquivo) {
					dir.add(new DirEntry(info[0], info[1], tipo, cluster, tam));
					gravaDirAtual();
					return true;
				} else {
					dir.add(new DirEntry(nome, null, tipo, cluster, -1));  
					gravaDirAtual();
					return true;
				}
			}
			
			return false;
		
		} catch(Exception e) {
			//e.printStackTrace();
			return false;
		}
	}
	
	
	// verifica se um arquivo ou dir de mesmo nome ja existe no dirAtual
	// RECEBE NOME DE ARQUIVO COM EXT!!
	public boolean jaExiste(String nome) {
		for (DirEntry de : this.dirAtual) {
			if (de.getNome().equalsIgnoreCase(nome))
				return true;
		}
		
		return false;
	}
	
	// RECEBE NOME DE ARQUIVO COM EXT!!
	public boolean jaExiste(String nome, ArrayList<DirEntry> dir) {
		for (DirEntry de : dir) {
			if (de.getNome().equalsIgnoreCase(nome))
				return true;
		}
		
		return false;
	}
	
	public boolean aumentaDirAtual() {
		int index = 0;
		for (int i = 21; i < quantClusters; i++) {
			if (this.vetFAT[i] == 9999) { // acha o proximo indice livre
				index = i; 
				break;
			}
		}
		
		if (index == 0)
			return false; // sai se nao tiver
		
		// se vetFAT[dirFAT] == 0, quer dizer que o diretorio so ocupava um cluster
		if (this.vetFAT[dirFAT] == 0) {
			this.vetFAT[dirFAT] = index;  // aponta pro proximo livre
			this.vetFAT[index] = 0;       // define proximo como fim
			atualizaFATnoDisco();
			return true;
		}
		
		// procura o ultimo indice do dirAtual
		int proximo = this.vetFAT[dirFAT];
		while (this.vetFAT[proximo] != 0) {       //pode ser interessante ter um metodo so pra isso
			proximo = this.vetFAT[proximo];
		}
		
		this.vetFAT[proximo] = index; // ultimo recebe o proximo livre
		this.vetFAT[index] = 0;       // define o proximo como fim
		atualizaFATnoDisco();
		return true;
	}
	
	public void gravaDirAtual() {
		
		int size = dirAtual.size();
		for (int i = 0; i < size * 24; i++) {
			buffer[i] = 0; // limpa previamente tudo que o buffer for usar.
		}
		
		DirEntry de;
		for (int i = 0; i < size; i++) { // para cada dirEntry em dirAtual, joga byte por byte no buffer
			de = dirAtual.get(i);
			
			byte[] nome = de.getNome().getBytes();
			for (int j = 0; j < nome.length; j++) {
				// se i = 0, vai 0 1 2 3 4 ... 23
				// se i = 1, vai 24 25 26 27 ... 47
				buffer[i*24+j] = nome[j];
			}
			
			if (de.getExt() != null) {
				byte[] ext = de.getExt().getBytes();
				for (int j = 0; j < ext.length; j++) {
					buffer[i*24+16+j] = ext[j];
				}	
			}
			
			buffer[i*24+19] = (byte) de.getTipo();
			
			int cluster = de.getPrimeiroCluster();
			buffer[i*24+20] = (byte) (cluster / 256);  
			buffer[i*24+21] = (byte) (cluster % 256);
			
			if (de.getTipo() == 'A') {
				int tamanho = de.getTamanho();
				buffer[i*24+22] = (byte) (tamanho / 256);
				buffer[i*24+23] = (byte) (tamanho % 256);
			}
		}
		
		// se so tiver 1 cluster, chama o metodo que grava apenas 1.
		if (this.vetFAT[dirFAT] == 0) {   
			writeClusterOnDisk(dirFAT, size * 24);
		} else {
			// se tiver mais de 1, monta um ArrayDeque<Integer> com todos os indices para o writeBufferOnDisk usar.
			ArrayDeque<Integer> indexes = new ArrayDeque<>(); 
			indexes.add(dirFAT);
			int proximo = this.vetFAT[dirFAT];
			do {
				indexes.add(proximo);  // adiciona indices ate o valor na FAT = 0.                  
				proximo = this.vetFAT[proximo];
			} while (proximo != 0);
			
			writeBufferOnDisk(indexes, size * 24);   // nao cobre casos de um diretorio que nao cabe no buffer.  
		}                                            // um diretorio com 200 entradas?
	}
	
	public boolean remove(int index) {
		DirEntry de;
		try {
			de = this.dirAtual.get(index);
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Indice invalido!");
			return false;
		}
		
		if (de.getTipo() == 'D') {
			removeDir(de);
			liberaNaFAT(de.getPrimeiroCluster());
			this.dirAtual.remove(index);
			gravaDirAtual();
			return true;
		} else {
			liberaNaFAT(de.getPrimeiroCluster());
			this.dirAtual.remove(index);
			gravaDirAtual();
			return true;
		}
		
		
	}
	
	@SuppressWarnings("unchecked")
	public void removeDir(DirEntry de) {
		this.dirFAT = de.getPrimeiroCluster();
		int tam = medeClusters(dirFAT);
		aumentaPath(de.getNome());
		montaDir(readDisk(this.dirFAT, tam), tam);
		ArrayList<DirEntry> dir = (ArrayList<DirEntry>) this.dirAtual.clone();
		
		DirEntry deSub;
		int size = dir.size();
		for (int i = 0; i < size; i++) {
			deSub = dir.get(i);
			if (deSub.getTipo() == 'D')
				removeDir(deSub);	
		}
		
		
		for (int i = 0; i < size; i++) {
			deSub = dir.remove(0);
			liberaNaFAT(deSub.getPrimeiroCluster());
		}
		
		this.dirAtual = dir;
		gravaDirAtual();
		
		carregaPastaPai();
	}
	
	public boolean copia(int index, String path) {
		String[] arrPath;
		if ((arrPath = pathToArray(path)) == null) {
			System.out.println("Caminho formatado incorretamente!");
			return false;
		}
		
		DirEntry de;
		try {
			de = this.dirAtual.get(index);
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Indice invalido!");
			return false;
		}
		
		String[] pathAnterior = this.dirPath.clone();
		
		if (de.getTipo() == 'D') {
			copiaDir(de, arrPath);
			System.out.println("Diretorio " + de.getNome() + " copiado para " + path);
			return true;
		}
		
		if (carregaPath(path)) {
			if (copiaArq(de, this.dirAtual)) {
				System.out.println("Arquivo " + de.getNome() + " copiado para " + path);
				return true;
			} else {
				carregaPath(pathAnterior);
				return false;
			}
		
		} else {
			carregaPath(pathAnterior);
			System.out.println("Caminho invalido!");
			return false;
		}	
	}
	
	public boolean copiaArq(DirEntry de, ArrayList<DirEntry> dir) {
		int tam = de.getTamanho();
		
		ArrayDeque<Integer> indexes;
		if((indexes = (ArrayDeque<Integer>) procuraLivres(getTeto(tam))) != null) {
			
			if (!insereCopia(de.getNomeComExt(), 'A', indexes.peek(), tam, dir)) {               // tenta inserir no dirAtual
				System.out.println("Nao foi possivel gravar o arquivo no disco virtual!");
				return false;
			}
			alocaNaFAT(indexes.clone()); // Pega os indices livres e encadeia na FAT		
		} else {
			System.out.println("Nao ha espaco suficiente!");
			return false;
		}
		
		byte[] arquivo = readDisk(de.getPrimeiroCluster(), getTeto(tam));
		boolean encheu = false;
		int i;
		for (i = 0; i < tam; i++) {   //enquanto nao chegar no final do arquivo...
			buffer[i%bufferSize] = arquivo[i];
			if(i == tam - 1 || i%bufferSize == 4799) {
				writeBufferOnDisk(indexes, i%bufferSize + 1);
				encheu = true;
			} else {
				encheu = false;
			}
		}

		//insere o que sobrou (se tiver sobrado)
		if (!encheu) {
			writeBufferOnDisk(indexes, (i%bufferSize) + 1);
		}
		
		return true;
	}
	
	public boolean copiaDir(DirEntry de, String[] path) {
		int tamanho = medeDirClusters(de) + medeClusters(de.getPrimeiroCluster());
		if (!temEspaco(tamanho)) {
			System.out.println("Nao ha espaco suficiente no disco para duplicar este diretorio.");
			return false;
		}
		
		if (!carregaPath(path)) {
			System.out.println("Caminho invalido!");
			return false;
		}
		
		if (!copiaDir(de, this.dirAtual)) {
			return false;
		}
		
		copiaDirRec(de);
		
		return true;
	}
	
	public boolean copiaDir(DirEntry de, ArrayList<DirEntry> dir) {
		int tam = medeClusters(de.getPrimeiroCluster());
		
		ArrayDeque<Integer> indexes;
		if((indexes = (ArrayDeque<Integer>) procuraLivres(tam)) != null) {
			
			if (!insereCopia(de.getNome(), 'D', indexes.peek(), tam, dir)) {               // tenta inserir no dirAtual
				System.out.println("Nao foi possivel gravar o arquivo no disco virtual!");
				return false;
			}
			alocaNaFAT(indexes.clone()); // Pega os indices livres e encadeia na FAT		
		} else {
			System.out.println("Nao foi possivel copiar o diretorio " + de.getNome() + " para o novo diretorio.");
			return false;
		}
		
		byte[] dirBytes = readDisk(de.getPrimeiroCluster(), tam);
		for (int i = 0; i < dirBytes.length; i++) {
			buffer[i] = dirBytes[i];
		}
		
		writeBufferOnDisk(indexes, dirBytes.length);
		
		return true;
	}
	
	@SuppressWarnings("unchecked")
	public void copiaDirRec(DirEntry de) {
		this.dirFAT = de.getPrimeiroCluster();
		int tam = medeClusters(dirFAT);
		aumentaPath(de.getNome());
		montaDir(readDisk(this.dirFAT, tam), tam);
		ArrayList<DirEntry> dir = (ArrayList<DirEntry>) this.dirAtual.clone();
		
		DirEntry deSub;
		int size = dir.size();
		for (int i = 0; i < size; i++) {
			deSub = dir.remove(0);
			if (deSub.getTipo() == 'A')
				copiaArq(deSub, dir);
			else {
				copiaDir(deSub, dir);
				copiaDirRec(deSub);
			}
		}
		
		this.dirAtual = dir;
		gravaDirAtual();
		
		carregaPastaPai();
	}
	
//	public int medeDirClusters(int index) {
//		DirEntry de;
//		try {
//			de = this.dirAtual.get(index);
//		} catch (IndexOutOfBoundsException e) {
//			System.out.println("Indice invalido!");
//			return -1;
//		}
//		
//		return medeDirClusters(de) + medeClusters(de.getPrimeiroCluster());
//		
//	}
	
	@SuppressWarnings("unchecked")
	public int medeDirClusters(DirEntry de) {
		int count = 0;
		this.dirFAT = de.getPrimeiroCluster();
		int tam = medeClusters(this.dirFAT);
		aumentaPath(de.getNome());
		montaDir(readDisk(this.dirFAT, tam), tam);
		ArrayList<DirEntry> dir = (ArrayList<DirEntry>) this.dirAtual.clone();
		
		DirEntry deSub;
		int size = dir.size();
		for (int i = 0; i < size; i++) {
			deSub = dir.get(i);
			if (deSub.getTipo() == 'D')
				count += medeDirClusters(deSub);	
		}
		
		for (int i = 0; i < size; i++) {
			deSub = dir.get(i);
			if (deSub.getTipo() == 'D')
				count += medeClusters(deSub.getPrimeiroCluster());
			else
				count += getTeto(deSub.getTamanho());
		}
		
		this.dirAtual = dir;
		
		carregaPastaPai();
		return count;
	}
	
	public void mostraDirAtual() {
		System.out.println(getPath() + ":");
		for(DirEntry de : dirAtual) {
			System.out.println(de); // toString()
		}
		System.out.println();
	}
	
	public void mostraDirMenu() {
        for(int i = 0; i < dirAtual.size(); i++) {
            System.out.println((i+1) + ". " + dirAtual.get(i));
        }
        System.out.println();
    }
	
	public void visualiza(int index) {
        DirEntry de;
        try {
            de = this.dirAtual.get(index);
        } catch (IndexOutOfBoundsException e) {
            System.out.println("Indice invalido!");
            return;
        }
    	
//        if(de.getTipo() == 'D') {
//        	
//        }
        
        byte[] arqBytes = readDisk(de.getPrimeiroCluster(), medeClusters(de.getPrimeiroCluster()));
        
        System.out.println("=======================================================================================================================");
        System.out.println("Visualizando "+ de.getTipoS() + " " + de.getNomeComExt() + ":\n");
        if (de.getTipo() == 'A') {
	        for (int i = 0; i < arqBytes.length; i++) {
	            if (arqBytes[i] == 0) {
	                System.out.println();
	                break;
	            }
	            
	            System.out.print(String.format("%02X ", arqBytes[i]));
	            if ((i+1) % 40 == 0 || i == arqBytes.length - 1) {
	                System.out.println();
	            }
	        }
        } else {
        	for (int i = 0; i < arqBytes.length; i++) {
	            if (arqBytes[i] == 0 && i % 24 == 0) {
	                System.out.println();
	                break;
	            }
	            System.out.print(String.format("%02X ", arqBytes[i]));
	            if ((i+1) % 24 == 0) {
	                System.out.println();
	            }
        	}
        }
        
        System.out.println("=======================================================================================================================");
    }
	
	public int getQuantAlocados() {
		int count = 0;
		for (int i = 0; i < this.quantClusters; i++) {
			if (this.vetFAT[i] != 9999) {
				count++;
			}
		}
		
		return count;
	}
	
	public int getBytesLivres() {
		int count = 0;
		for (int i = 0; i < this.quantClusters; i++) {
			if (this.vetFAT[i] == 9999) {
				count++;
			}
		}
		
		return count * 240;
	}
	
	public void getTotalDeTudo() {
		String[] pathAnterior = this.dirPath.clone();
		carregaRaiz();
		int[] countArqDir = getTotalRec();
		System.out.println("Disco virtual contem " + countArqDir[0] + " arquivos em " + (countArqDir[1] + 1) + " diretorios.");
		carregaPath(pathAnterior);
	}
	
	@SuppressWarnings("unchecked")
	public int[] getTotalRec() {
		int[] count = new int[2];
		
		ArrayList<DirEntry> dir = (ArrayList<DirEntry>) this.dirAtual.clone();
		
		DirEntry deSub;
		int[] aux = new int[2];
		int size = dir.size();
		for (int i = 0; i < size; i++) {
			deSub = dir.get(i);
			if (deSub.getTipo() == 'D') {
				carregaSubpasta(i);
				aux = getTotalRec();
				count[0] += aux[0];
				count[1] += aux[1];
			}
		}
		
		for (int i = 0; i < size; i++) {
			deSub = dir.get(i);
			if (deSub.getTipo() == 'A')
				count[0]++;
			else
				count[1]++;
		}
		
		this.dirAtual = dir;	
		
		if (!getNomeAtual().equalsIgnoreCase("disco"))
            carregaPastaPai();
		
		return count;
	}
	
    public void testa() {
    	
    	inicializaFAT();
    		
    	//gravando e lendo um cluster
    	byte[] cluster = new byte[clusterSize];
    	String s = "este eh o conteudo de um cluster...";
    	cluster = s.getBytes();
    	d.writeCluster(cluster, 22);
    	 	
    	byte[] clusterLido = new byte[clusterSize]; 	
    	clusterLido = d.readCluster(22);    	
    	System.out.println(clusterLido[0]);
    	
    }
    
    public void mostraFAT() {
    	System.out.println("--- Mostra FAT ---");
    	
		for(int i = 0; i < vetFAT.length; i++) {
			System.out.println(i + ": " + vetFAT[i]);
		}
		
		System.out.println("------------------");
    }
    
    public void mostraFAT(int lim) {
    	System.out.println("--- Mostra FAT ---");
    	
		for(int i = 0; i < lim; i++) {
			System.out.println(i + ": " + vetFAT[i]);
		}
		
		System.out.println("------------------");
    }
    
    public void navegacao() {
        int i = t.leInt("Que acao deseja executar?\n"
        		+ "1 - Navegar para subpastas por indice.\n"
        		+ "2 - Digitar o nome de uma subpasta.\n"
                + "3 - Voltar para o diretorio anterior.\n"
                + "4 - Digitar o path.\n"
                + "0 - Voltar para o menu.");
        switch(i) {
        case 1:
        	mostraDirMenu();
        	carregaSubpasta(t.leInt("Qual subpasta deseja acessar?")-1);
        	break;
        case 2:
        	mostraDirAtual();
        	carregaSubpasta(t.leString("Qual o nome da subpasta que deseja acessar?"));
        	break;
        case 3:
            carregaPastaPai();
            break;
        case 4: 
        	carregaPath(t.leString("Digite o path para aonde deseja ir:"));
        	break;
        case 0:
            carregaRaiz();
            break;
        default:
        	System.out.println("Valor invalido.");
        	break;
        }        
    }
    
    public void inicializaProjeto() {
        
        inicializaFAT();
        
        System.out.println("Bem vindo(a)!");
        carregaRaiz();
        
        int i = -1; 
        
        while(i != 9) {

            System.out.println();
            mostraDirAtual();
            
            i = t.leInt("Digite a acao que deseja executar:\n"
                    + "0 - Navegar.\n"
                    + "1 - Criar Diretorio.\n"
                    + "2 - Importar Arquivo.\n"
                    + "3 - Remover arquivo ou diretorio.\n"
                    + "4 - Copiar arquivo ou diretorio.\n"
                    + "5 - Mover arquivo ou diretorio.\n"
                    + "6 - Visualizar bytes de arquivo.\n"
                    + "7 - Verificar quantidade de arquivos/diretorios.\n" 
                    + "8 - Mostra FAT.\n"
                    + "9 - Fechar Disco.");
            
            switch(i) {
	            case 0:
	                navegacao();
	                break;
	            case 1:
	                criaDiretorio(t.leString("Digite o nome do Diretorio:"));
	                break;
	            case 2:
	                importaArquivo(t.leString("Digite o nome do Arquivo(com extensao):"));
	                break;
	            case 3:
	            	mostraDirMenu();
	            	remove(t.leInt("O que deseja remover?")-1);
	                break;
	            case 4: 
	            	mostraDirMenu();
	
	                int ind = t.leInt("O que deseja deseja copiar?")-1;
	                String p = t.leString("Digite o path para aonde deseja copiar a entrada:");
	                copia(ind, p);
	                break;
	            case 5:
	                mostraDirMenu();
                
	                int index = t.leInt("O que deseja mover?")-1;
	                String[] path = pathToArray(t.leString("Digite o path para aonde deseja mover a entrada:"));
	                while(path == null) {
	                    path = pathToArray(t.leString("Path formatado incorretamente. Digite novamente:\n"
	                            + "Digite 'disco' para retornar ao diretorio raiz."));
	                }
	                move(index, path);
	                break;
	            case 6:
	            	mostraDirMenu();
	            	visualiza(t.leInt("O que deseja visualizar?")-1);
	            	break;
	            case 7:
	            	getTotalDeTudo();
	            	break;
	            case 8:
	            	mostraFAT();
	            	break;
	            case 9: 
	                fechaDisco();
	                break;
	            default:
	            	System.out.println("Valor invalido.");
	            	break;
            }
        }
    }
    
    public void fechaDisco() {
        d.closeDisk();
        
        System.out.println("Disco fechado.");
    }
}
