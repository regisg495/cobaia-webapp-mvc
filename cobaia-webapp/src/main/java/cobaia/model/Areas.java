package cobaia.model;

import cobaia.anottation.ValidaRegex;

public class Areas {
	@ValidaRegex("[a-zA-Z]+")
	private String nome;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public Areas(String nome) {
		this.nome = nome;
	}
	public Areas() {
		
	}
}
