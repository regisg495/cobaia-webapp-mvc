package cobaia.datamapper;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public abstract class Conexao {

	protected final String SALT = "dao";
	protected final String URL = "jdbc:sqlite:db/mochinho.sqlite";

	protected Connection openConnection() throws SQLException {
		return DriverManager.getConnection(URL);
	}
}
