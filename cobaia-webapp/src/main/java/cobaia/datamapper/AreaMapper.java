package cobaia.datamapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cobaia.anottation.ValidaImpl;
import cobaia.exception.InvalidValueException;
import cobaia.model.Areas;

public class AreaMapper extends Conexao {

	public void insert(Areas areas) throws IllegalArgumentException, IllegalAccessException {
		ValidaImpl validaImpl = new ValidaImpl();
		
		if(!validaImpl.valida(areas)) {
			throw new InvalidValueException("Not Valid");
		}
		try (Connection connection = openConnection()) {

			String sql = "INSERT INTO areas (nome) VALUES (?);";
			PreparedStatement stmt = connection.prepareStatement(sql);

			if (areas.getNome() == null) {
				stmt.setNull(1, Types.NULL);
			}

			else {
				stmt.setString(1, areas.getNome());
			}
			stmt.execute();

			connection.close();

		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

}
