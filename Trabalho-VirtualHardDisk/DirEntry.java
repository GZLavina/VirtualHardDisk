
public class DirEntry {

	private String nome;
	private String ext;
	private char tipo;
	private int primeiroCluster;
	private int tamanho;
	
	public DirEntry(String nome, String ext, char tipo, int cluster, int tam) {
		this.nome = nome;
		this.ext = ext;
		this.tipo = tipo;
		this.primeiroCluster = cluster;
		this.tamanho = tam;
	}
	
	

	public String getNome() {
		return nome;
	}



	public void setNome(String nome) {
		this.nome = nome;
	}



	public String getExt() {
		return ext;
	}



	public void setExt(String ext) {
		this.ext = ext;
	}



	public char getTipo() {
		return tipo;
	}

	public String getTipoS() {
		if (this.tipo == 'A')
			return "arquivo";
		else
			return "diretorio";
	}

	public void setTipo(char tipo) {
		this.tipo = tipo;
	}



	public int getPrimeiroCluster() {
		return primeiroCluster;
	}



	public void setPrimeiroCluster(int primeiroCluster) {
		this.primeiroCluster = primeiroCluster;
	}



	public int getTamanho() {
		return tamanho;
	}

	public String getTamStr() {
		if (tipo == 'A')
			return String.format("%5d bytes", tamanho);
		else
			return "    -";
	}

	public void setTamanho(int tamanho) {
		this.tamanho = tamanho;
	}

	public String getNomeComExt() {
		if (this.tipo == 'A')
			return nome + "." + ext;
		else
			return nome;
		
	}

	@Override
	public String toString() {
		String str = String.format("%-19s | Tipo: %-9s | Cluster inicial: %4d | Tamanho: %s", getNomeComExt(), getTipoS(), primeiroCluster, getTamStr());
		return str;
	}
}
