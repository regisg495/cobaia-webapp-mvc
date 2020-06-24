package cobaia.model;

import cobaia.anottation.ValidaNotNull;
import cobaia.anottation.ValidaLength;

public class ObjectDAOTest {

	// Eu criei essa classe apenas para teste dos dados, como enum, data, no genéric
	// DAO
	// É só uma classezinha pra jogar um monte de tipos e poder testar se funciona o
	// genérico
	// uma vez que as classes Areas e Usuarios têm apenas 1 field de tipo VARCHAR
	@ValidaLength(min = 5, max = 20)
	private String nome;
	
	@ValidaLength(min = 1, max = 1)
	private char sexo;

	@ValidaNotNull
	private Integer idade;
	
	private int numeroDaSorte;
	private Double salario;
	private double nota1;
	private Float peso;
	private float nota2;
	private Professor professorfavorito;

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public char getSexo() {
		return sexo;
	}

	public void setSexo(char sexo) {
		this.sexo = sexo;
	}

	public Integer getIdade() {
		return idade;
	}

	public void setIdade(Integer idade) {
		this.idade = idade;
	}

	public int getNumeroDaSorte() {
		return numeroDaSorte;
	}

	public void setNumeroDaSorte(int numeroDaSorte) {
		this.numeroDaSorte = numeroDaSorte;
	}

	public Double getSalario() {
		return salario;
	}

	public void setSalario(Double salario) {
		this.salario = salario;
	}

	public double getNota1() {
		return nota1;
	}

	public void setNota1(double nota1) {
		this.nota1 = nota1;
	}

	public Float getPeso() {
		return peso;
	}

	public void setPeso(Float peso) {
		this.peso = peso;
	}

	public float getNota2() {
		return nota2;
	}

	public void setNota2(float nota2) {
		this.nota2 = nota2;
	}

	public Professor getProfessorfavorito() {
		return professorfavorito;
	}

	public void setProfessorfavorito(Professor professorfavorito) {
		this.professorfavorito = professorfavorito;
	}

	public ObjectDAOTest(String nome, char sexo, Integer idade, int numeroDaSorte, Double salario, double nota1,
			Float peso, float nota2, Professor professorfavorito) {
		super();
		this.nome = nome;
		this.sexo = sexo;
		this.idade = idade;
		this.numeroDaSorte = numeroDaSorte;
		this.salario = salario;
		this.nota1 = nota1;
		this.peso = peso;
		this.nota2 = nota2;
		this.professorfavorito = professorfavorito;
	}

}
