package cobaia.datamapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import cobaia.anottation.ValidaImpl;
import cobaia.exception.InvalidValueException;
import cobaia.model.Usuarios;

public class UsuarioMapper extends Conexao {
		public void insert(Usuarios usuario) throws IllegalArgumentException, IllegalAccessException {
			ValidaImpl validaImpl = new ValidaImpl();
			if(!validaImpl.valida(usuario)) {
				throw new InvalidValueException("Invalid value");
			}
			try(Connection connection = openConnection()){
				String sql  = "INSERT INTO usuarios (nome, email, senha) VALUES (?,?,?);";
				PreparedStatement stmt = connection.prepareStatement(sql);
				
				if(usuario.getNome() == null) {
					stmt.setNull(1, Types.NULL);
				} else {
					stmt.setString(1, usuario.getNome());
				}
				if(usuario.getEmail() == null) {
					stmt.setNull(2, Types.NULL);
				} else {
					stmt.setString(2, usuario.getEmail());
				} 
				if(usuario.getSenha() == null) {
					stmt.setNull(3, Types.NULL);
				} else {
					stmt.setString(3, usuario.getSenha());
				}
				
				stmt.execute();
				connection.close();
				
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}
}
