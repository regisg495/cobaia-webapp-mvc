package cobaia.model;

import cobaia.anottation.ValidaRegex;

public class Usuarios {

	enum Status {
		Desativado, Ativado
	}

	@ValidaRegex("[A-Z][a-z]+( [A-Z][a-z]+)+")
	private String nome;
	@ValidaRegex("[a-z]+@[a-z]+\\.(com|edu|mil|gov|org|gmail|hotmail|live|ifrs)(\\.[a-z]{2})?")
	private String email;
	@ValidaRegex("^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&*-]).{8,}$")
	private String senha;

	private Status status = Status.Desativado;
	private String token;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getSenha() {
		return senha;
	}

	public void setSenha(String senha) {
		this.senha = senha;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Usuarios() {

	}


}
