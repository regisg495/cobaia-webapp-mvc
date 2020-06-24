package cobaia;
	
//#region imports
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.Part;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

import cobaia.ORM.ORMAutoConvention;
import cobaia.model.Areas;
import cobaia.model.Usuarios;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;
import spark.TemplateEngine;
import spark.debug.DebugScreen;
import spark.template.pebble.PebbleTemplateEngine;
//#endregion

public class Main {

	public static void main(String[] args) {

		final String SALT = "cobaia";

		Spark.staticFileLocation("/public");
		final TemplateEngine pebble = new PebbleTemplateEngine();
		DebugScreen.enableDebugScreen();
		SimpleDateFormat ISODateFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat ISOTimeFormat = new SimpleDateFormat("hh:mm");

		Spark.get("/", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();

				/* PASSAR O USUÃ�RIO SE PRESENTE NA SESSÃƒO PARA A VIEW */
				if (req.session().attribute("usuario") != null) {
					map.put("usuario", req.session().attribute("usuario").toString());
					map.put("email", req.session().attribute("email").toString());
				}

				return pebble.render(new ModelAndView(map, "templates/index.pebble"));
			}
		});

		Spark.get("/login", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				if (req.session().attribute("email") != null)
					map.put("email", req.session().attribute("email").toString());
				if (req.session().attribute("codigo") != null)
					map.put("codigo", req.session().attribute("codigo").toString());
				return pebble.render(new ModelAndView(map, "templates/login.pebble"));
			}
		});

		Spark.post("/login", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				String email = req.queryParams("email");
				map.put("email", email);
				String senha = req.queryParams("senha");
				boolean encontrado = false;
				try {

					Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite");
					String sql = "SELECT id, nome, email, status FROM usuarios WHERE email = ? AND senha = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setString(1, email);
					stmt.setString(2, DigestUtils.md5Hex(senha + SALT));
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						if (rs.getInt("status") == 0) {
							map.put("erro",
									"Conta não está ativada, digite o código recebido no e-mail para ativa-la");
							return pebble.render(new ModelAndView(map, "templates/ativar.pebble"));
						}
						req.session().attribute("id", rs.getInt("id"));
						req.session().attribute("usuario", rs.getString("nome"));
						req.session().attribute("email", rs.getString("email"));
						encontrado = true;
					}
					con.close();
				} catch (Exception sqle) {
					throw new RuntimeException(sqle);
				}
				if (encontrado) {
					resp.redirect("/perfil");
				} else {
					map.put("erro", "E-mail e/ou senha não encontrados");
					return pebble.render(new ModelAndView(map, "templates/login.pebble"));
				}
				return "OK";
			}
		});

		Spark.get("/db/seed", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				String url = "jdbc:sqlite:db/mochinho.sqlite";
				try (Connection con = DriverManager.getConnection(url)) {
					con.createStatement().execute(
							"INSERT INTO usuarios (nome, email, senha, status) VALUES ('Seu Madruga', 'madruga@chaves.mx', 'madruga', 1)");
					con.createStatement().execute(
							"INSERT INTO areas (nome) VALUES ('Artes'), ('Beleza'), ('ComunicaÃ§Ã£o'), ('InformÃ¡tica'), ('Gastronomia'), ('Idiomas'), ('Moda'), ('SaÃºde')");
					con.createStatement().execute(
							"INSERT INTO cursos (nome, vagas, data_inicio, data_termino, dias, horario_inicio, horario_termino, carga_horaria, id_area) VALUES ('Petit Gateu AvanÃ§ado', 15, '2018-10-01', '2018-11-30', 'seg', '19:00', '22:00', 90, (SELECT id FROM areas WHERE nome = 'Gastronomia'))");
					return true;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		Spark.get("/db/create", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				String url = "jdbc:sqlite:db/mochinho.sqlite";
				try (Connection con = DriverManager.getConnection(url)) {
					con.createStatement().execute(
							"CREATE TABLE IF NOT EXISTS usuarios (id INTEGER NOT NULL PRIMARY KEY, nome VARCHAR(50) NOT NULL, email VARCHAR(50) NOT NULL, senha VARCHAR(32) NOT NULL, status INTEGER DEFAULT 0 NOT NULL, token CHAR(36) NULL)");
					con.createStatement().execute(
							"CREATE TABLE IF NOT EXISTS areas (id INTEGER NOT NULL PRIMARY KEY, nome VARCHAR(20) NOT NULL);");
					con.createStatement().execute(
							"CREATE TABLE IF NOT EXISTS cursos (id INTEGER NOT NULL PRIMARY KEY, nome VARCHAR(50) NOT NULL, resumo VARCHAR(100), programa VARCHAR(500), vagas INTEGER NOT NULL, data_inicio DATE NOT NULL, data_termino DATE NOT NULL, dias VARCHAR(28) NOT NULL, horario_inicio TIME NOT NULL, horario_termino TIME NOT NULL, carga_horaria INTEGER NOT NULL, imagem BLOB, tipo_imagem VARCHAR(3), id_area INTEGER NOT NULL REFERENCES areas (id))");
					con.createStatement().execute(
							"CREATE TABLE IF NOT EXISTS inscricoes (id_usuario INTEGER NOT NULL, id_curso INTEGER NOT NULL, concluiu BOOLEAN DEFAULT FALSE NOT NULL, CONSTRAINT inscricao_pk PRIMARY KEY (id_usuario, id_curso))");
					return true;
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		Spark.get("/db/drop", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				try {
					return Files.deleteIfExists(Paths.get("db/mochinho.sqlite"));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});

		Spark.get("/registro", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
			}
		});

		Spark.get("/perfil", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				if (req.session().attribute("usuario") == null) {
					Spark.halt(401, "nao autorizado");
				} else {
					map.put("usuario", req.session().attribute("usuario").toString());
					map.put("email", req.session().attribute("email").toString());
				}
				return pebble.render(new ModelAndView(map, "templates/perfil.pebble"));
			}
		});

		Spark.get("/mailer", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {

				Map<String, String> map = new HashMap<>();
				HtmlEmail mailer = new HtmlEmail();
				try {
					mailer.setHostName("smtp.googlemail.com");
					mailer.setSmtpPort(465);
					mailer.setAuthenticator(
							new DefaultAuthenticator("nao.responda.ifrs.riogrande@gmail.com", "senha-do-email"));
					mailer.setSSLOnConnect(true);
					mailer.setFrom("nao.responda.ifrs.riogrande@gmail.com");
					mailer.setSubject("[PING] nao.responda.ifrs.riogrande@gmail.com FROM cobaia-webapp");
					mailer.setHtmlMsg("PONG!");
					mailer.addTo("marcio.torres@riogrande.ifrs.edu.br");
					mailer.send();
				} catch (EmailException e) {
					throw new RuntimeException(e);
				}
				return "OK";
			}
		});

		Spark.get("/reenviar", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				// map.put("info", "VocÃª precisa digitar o cÃ³digo recebido por e-mail para
				// ativar sua conta");
				return pebble.render(new ModelAndView(map, "templates/reenviar.pebble"));
			}
		});

		Spark.post("/reenviar", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				String email = req.queryParams("email");
				String nome = null;
				map.put("email", email);

				if (!email.matches("[\\w._]+@\\w+(\\.\\w+)+")) {
					map.put("erro", "E-mail inválido, ele deve ter o formato de usuario@provedor");
					return pebble.render(new ModelAndView(map, "templates/reenviar.pebble"));
				}

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String uid = UUID.randomUUID().toString().split("-")[0];

					con.setAutoCommit(false);

					String sql = "SELECT status, nome FROM usuarios WHERE email = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setString(1, email);
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						nome = rs.getString("nome");
						if (rs.getInt("status") > 0) {
							map.put("info", "Esta conta já está ativada, você pode fazer o login.");
							return pebble.render(new ModelAndView(map, "templates/login.pebble"));
						}
					} else {
						map.put("erro", "Este e-mail não existe no nosso sistema, você pode fazer o cadastro.");
						return pebble.render(new ModelAndView(map, "templates/reenviar.pebble"));
					}

					HtmlEmail mailer = new HtmlEmail();
					mailer.setHostName("smtp.googlemail.com");
					mailer.setSmtpPort(465);
					mailer.setAuthenticator(new DefaultAuthenticator("nao.responda.ifrs.riogrande@gmail.com",
							System.getenv("COBAIA_MAIL_PASSWORD")));
					mailer.setSSLOnConnect(true);
					mailer.setFrom("nao.responda.ifrs.riogrande@gmail.com");
					mailer.setSubject("[Cobaia] Confirmar seu registro");
					mailer.setHtmlMsg("Olá " + nome + "<br><br>Confirme sua conta com esse código: " + uid
							+ " ou, se preferir, clique nesse link: <a href=\"http://localhost:4567/ativar/" + uid
							+ "\">http://localhost:4567/ativar/" + uid + "</a> para direciona-lo diretamente");
					mailer.addTo(email);
					mailer.send();

					con.commit();

					return pebble.render(new ModelAndView(map, "templates/ativar.pebble"));

				} catch (Throwable ex) {
					if (ex instanceof UnknownHostException) {
						map.put("erro", "O provedor deste e-mail nÃ£o foi encontrado, confira o endereÃ§o por favor");
						return pebble.render(new ModelAndView(map, "templates/reenviar.pebble"));
					}
					throw new RuntimeException(ex);
				}
			}
		});

		Spark.get("/ativar", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				map.put("info", "Você precisa digitar o código recebido por e-mail para ativar sua conta");
				return pebble.render(new ModelAndView(map, "templates/ativar.pebble"));
			}
		});

		Spark.post("/ativar", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();

				String codigo = req.queryParams("codigo");
				String email = req.queryParams("email");
				int ativado = 0;

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "UPDATE usuarios SET status = 1, token = NULL "
							+ "WHERE token = ? AND email = ? AND status = 0";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setString(1, codigo);
					stmt.setString(2, email);
					// BUG: nÃ£o estÃ¡ vindo o nro de rows atualizadas (hsql?)
					ativado = stmt.executeUpdate();
					con.close();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				if (ativado > 0) {
					map.put("info", "Sua conta foi ativada! Entre com seu e-mail e senha para fazer o login.");
					return pebble.render(new ModelAndView(map, "templates/login.pebble"));
				} else {
					map.put("erro",
							"Código não encontrado. Talvez você já tenha ativado sua conta. Tente fazer o login."
									+ ativado);
					return pebble.render(new ModelAndView(map, "templates/ativar.pebble"));
				}
			}
		});

		Spark.get("/ativar/:codigo", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				String email = null;
				String codigo = req.params("codigo");

				try {
					Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite");
					String sql = "SELECT email FROM usuarios WHERE token = ? AND status = 0";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setString(1, codigo);
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						email = rs.getString("email");
					}
					con.close();
				} catch (Exception sqle) {
					throw new RuntimeException(sqle);
				}

				if (email == null) {
					map.put("erro", "Esta conta jÃ¡ foi ativada, tente fazer o login");
				} else {
					map.put("email", email);
				}

				map.put("codigo", codigo);
				return pebble.render(new ModelAndView(map, "templates/ativar.pebble"));
			}
		});

		Spark.post("/registro", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();

				Usuarios novoUsuario = new Usuarios();

				novoUsuario.setNome(req.queryParams("nome"));
				novoUsuario.setEmail(req.queryParams("email"));
				novoUsuario.setSenha(req.queryParams("senha"));

				String confirmacaoSenha = req.queryParams("senha2");

				map.put("novoUsuario", novoUsuario);

				if (novoUsuario.getNome().length() < 3 || novoUsuario.getNome().length() > 50) {
					map.put("erro", "Nome deve ter entre 3 e 50 caracteres");
					return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
				}

				if (!novoUsuario.getEmail().matches("[\\w._]+@\\w+(\\.\\w+)+")) {
					map.put("erro", "E-mail inválido, ele deve ter o formato de usuario@provedor");
					return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
				}

				if (novoUsuario.getSenha().length() < 5 || novoUsuario.getSenha().length() > 50) {
					map.put("erro", "A sua senha deve ter entre 5 e 50 caracteres");
					return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
				}

				if (!novoUsuario.getSenha().equals(confirmacaoSenha)) {
					map.put("erro", "As senhas nÃ£o conferem");
					return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
				}

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					con.setAutoCommit(false);

					String uid = UUID.randomUUID().toString().split("-")[0];

					PreparedStatement stmt = con.prepareStatement("SELECT status FROM usuarios WHERE email = ?");
					stmt.setString(1, novoUsuario.getEmail());
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						map.put("erro", "Este e-mail já está cadastrado.");
						return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
					}

					String sql = "INSERT INTO usuarios (nome, email, senha, token) VALUES (?, ?, ?, ?)";
					stmt = con.prepareStatement(sql);
					stmt.setString(1, novoUsuario.getNome());
					stmt.setString(2, novoUsuario.getEmail());
					stmt.setString(3, DigestUtils.md5Hex(novoUsuario.getSenha() + SALT));
					stmt.setString(4, uid);
					stmt.execute();

					HtmlEmail mailer = new HtmlEmail();
					mailer.setHostName("smtp.googlemail.com");
					mailer.setSmtpPort(465);
					mailer.setAuthenticator(
							new DefaultAuthenticator("nao.responda.ifrs.riogrande@gmail.com", "senha-do-email"));
					mailer.setSSLOnConnect(true);
					mailer.setFrom("nao.responda.ifrs.riogrande@gmail.com");
					mailer.setSubject("[Cobaia] Confirmar seu registro");
					mailer.setHtmlMsg("Olá " + novoUsuario.getNome() + "<br><br>Confirme sua conta com esse código: "
							+ uid + " ou, se preferir, clique nesse link: <a href=\"http://localhost:4567/ativar/" + uid
							+ "\">http://localhost:4567/ativar/" + uid + "</a> para direciona-lo diretamente");
					mailer.addTo(novoUsuario.getEmail());
					//mailer.send();

					con.commit();

					resp.redirect("/ativar");
					return "OK";
				} catch (Exception ex) {
					if (ex instanceof UnknownHostException) {
						map.put("erro", "O provedor deste e-mail não foi encontrado, confira o endereço por favor");
						return pebble.render(new ModelAndView(map, "templates/registro.pebble"));
					}
					throw new RuntimeException(ex);
				}
			}
		});

		Spark.get("/cursos", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "SELECT * FROM cursos LIMIT 10";
					PreparedStatement stmt = con.prepareStatement(sql);
					ResultSet rs = stmt.executeQuery();
					List<Map<String, Object>> cursos = new ArrayList<>();
					map.put("cursos", cursos);
					while (rs.next()) {
						Map<String, Object> curso = new HashMap<>();
						curso.put("nome", rs.getString("nome"));
						curso.put("id", rs.getInt("id"));
						curso.put("resumo", rs.getString("resumo"));
						cursos.add(curso);
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				return pebble.render(new ModelAndView(map, "templates/cursos.pebble"));
			}
		});

		Spark.get("/curso/:id", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();

				/* PASSAR O USUÃ�RIO SE PRESENTE NA SESSÃƒO PARA A VIEW */
				if (req.session().attribute("usuario") != null) {
					map.put("usuario", req.session().attribute("usuario").toString());
					map.put("email", req.session().attribute("email").toString());
				}

				int id = req.params("id") == null ? 0 : Integer.parseInt(req.params("id"));

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "SELECT c.*, a.nome AS area, (SELECT COUNT(*) FROM inscricoes WHERE id_curso = c.id) AS inscritos FROM cursos AS c JOIN areas AS a ON c.id_area = a.id WHERE c.id = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setInt(1, id);
					ResultSet rs = stmt.executeQuery();

					if (rs.next()) {
						Map<String, Object> curso = new HashMap<>();
						curso.put("id", rs.getInt("id"));
						curso.put("nome", rs.getString("nome"));
						curso.put("resumo", rs.getString("resumo"));
						curso.put("vagas", rs.getInt("vagas"));
						curso.put("cargaHoraria", rs.getInt("carga_horaria"));
						curso.put("dataInicio", rs.getDate("data_inicio").toLocalDate()
								.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
						curso.put("dataTermino", rs.getDate("data_Termino").toLocalDate());
						curso.put("dias", rs.getString("dias"));
						curso.put("horarioInicio", rs.getTime("horario_inicio").toLocalTime());
						curso.put("horarioTermino", rs.getTime("horario_termino").toLocalTime());
						curso.put("programa", rs.getString("programa"));
						curso.put("area", rs.getString("area"));
						curso.put("temImagem", rs.getString("tipo_imagem") != null);
						curso.put("inscritos", rs.getInt("inscritos"));
						map.put("curso", curso);
					} else {
						resp.status(404);
						return "Curso " + id + " nÃ£o encontrado";
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				return pebble.render(new ModelAndView(map, "templates/curso.pebble"));
			}
		});

		Spark.get("/inscrever/:id", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();

				int id = req.params("id") == null ? 0 : Integer.parseInt(req.params("id"));

				if (id == 0) {
					resp.status(401);
					return "Código do curso não informado";
				}

				/* PASSAR O USUÃ�RIO SE PRESENTE NA SESSÃƒO PARA A VIEW */
				if (req.session().attribute("usuario") == null) {
					Spark.halt(401, "nao autorizado");
				}

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {
					String sql = "INSERT INTO inscricoes " + "(id_usuario, id_curso) VALUES (?, ?);";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setInt(1, (Integer) req.session().attribute("id"));
					stmt.setInt(2, id);
					stmt.execute();

				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}

				resp.redirect("/curso/" + id);
				// return pebble.render(new ModelAndView(map, "templates/curso.pebble"));
				return "OK";
			}
		});

		Spark.get("/admin", new Route() {

			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();

				return pebble.render(new ModelAndView(map, "templates/admin.pebble"));
			}
		});

		Spark.get("/curso/imagem/:id", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				int id = req.params("id") == null ? 0 : Integer.parseInt(req.params("id"));

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "SELECT imagem, tipo_imagem FROM cursos WHERE id = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setInt(1, id);
					ResultSet rs = stmt.executeQuery();
					if (rs.next()) {
						String contentType = "image/"
								+ (rs.getString("tipo_imagem").equals("jpeg") ? "jpg" : rs.getString("tipo_imagem"));
						InputStream inputStream = rs.getBinaryStream("imagem");
						OutputStream outputStream = resp.raw().getOutputStream();
						resp.raw().setContentType(contentType);
						IOUtils.copy(inputStream, outputStream);
						return null;
					} else {
						resp.status(404);
						return "Imagem " + id + " não encontrada";
					}
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});

		Spark.post("/admin/curso/novo", new Route() {

			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
				String nome = req.queryParams("nome");
				if (nome == null) {
					map.put("erro", "Nome não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				String resumo = req.queryParams("resumo");
				if (resumo == null) {
					map.put("erro", "Resumo não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				int vagas = req.queryMap("vagas").integerValue();
				if (vagas < 1) {
					map.put("erro", "Quantitativo de vagas não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				int cargaHoraria = req.queryMap("carga_horaria").integerValue();
				if (cargaHoraria < 1) {
					map.put("erro", "Carga horária não informada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				if (req.queryParams("data_inicio") == null) {
					map.put("erro", "Dada de início não informada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				Date dataInicio = ISODateFormat.parse(req.queryParams("data_inicio"));
				if (req.queryParams("data_termino") == null) {
					map.put("erro", "Dada de termino não informada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				Date dataTermino = ISODateFormat.parse(req.queryParams("data_termino"));
				String dias = String.join(", ", req.queryParamsValues("dias"));
				if (req.queryParams("horario_inicio") == null) {
					map.put("erro", "Horário de início não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				Date horaInicio = ISOTimeFormat.parse(req.queryParams("horario_inicio"));
				if (req.queryParams("horario_termino") == null) {
					map.put("erro", "Horário de termino nÃ£o informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}
				Date horaTermino = ISOTimeFormat.parse(req.queryParams("horario_termino"));
				String programa = req.queryParams("programa");
				Part imagem = req.raw().getPart("imagem");
				InputStream imagemInputStream = null;
				String tipoImagem = null;
				if (imagem.getSize() > 0) {
					imagemInputStream = imagem.getInputStream();
					tipoImagem = imagem.getContentType().split("/")[1];
					// Image reader = new Image(imagemInputStream);
					// return reader.getWidth() + "/" + reader.getHeight();
				}

				if (req.queryParams("area") == null) {
					map.put("erro", "Área não selecionada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
				}

				int idArea = req.queryMap("area").integerValue();

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {
					String sql = "INSERT INTO cursos "
							+ "(nome, resumo, vagas, carga_horaria, data_inicio, data_termino, dias, horario_inicio, horario_termino, programa, imagem, tipo_imagem, id_area) "
							+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setString(1, nome);
					stmt.setString(2, resumo);
					stmt.setInt(3, vagas);
					stmt.setInt(4, cargaHoraria);
					stmt.setDate(5, new java.sql.Date(dataInicio.getTime()));
					stmt.setDate(6, new java.sql.Date(dataTermino.getTime()));
					stmt.setString(7, dias);
					stmt.setTime(8, new java.sql.Time(horaInicio.getTime()));
					stmt.setTime(9, new java.sql.Time(horaTermino.getTime()));

					if (programa == null || programa.isEmpty()) {
						stmt.setNull(10, Types.VARCHAR);
					} else {
						stmt.setString(10, programa);
					}

					if (imagemInputStream == null) {
						stmt.setNull(11, Types.BLOB);
					} else {
						stmt.setBlob(11, imagemInputStream);
					}

					if (tipoImagem == null) {
						stmt.setNull(12, Types.VARCHAR);
					} else {
						stmt.setString(12, tipoImagem);
					}

					stmt.setInt(13, idArea);

					stmt.execute();

					resp.redirect("/admin");
					return "OK";
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});

		Spark.get("/admin/curso/novo", new Route() {

			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();
				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {
					String sql = "SELECT id, nome FROM areas ORDER BY nome";
					ResultSet rs = con.prepareStatement(sql).executeQuery();
					List<Map<String, Object>> areas = new ArrayList<>();
					while (rs.next()) {
						Map<String, Object> area = new HashMap<>();
						area.put("id", rs.getInt("id"));
						area.put("nome", rs.getString("nome"));
						areas.add(area);
					}
					map.put("areas", areas);
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
				return pebble.render(new ModelAndView(map, "templates/admin.curso.novo.pebble"));
			}
		});

		Spark.get("/admin/cursos", new Route() {

			@Override
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "SELECT c.*, a.nome AS area, (SELECT COUNT(*) FROM inscricoes WHERE id_curso = c.id) AS inscritos FROM cursos AS c JOIN areas AS a ON c.id_area = a.id LIMIT 10";
					PreparedStatement stmt = con.prepareStatement(sql);
					ResultSet rs = stmt.executeQuery();
					List<Map<String, Object>> cursos = new ArrayList<>();
					map.put("cursos", cursos);
					while (rs.next()) {
						Map<String, Object> curso = new HashMap<>();
						curso.put("id", rs.getInt("id"));
						curso.put("nome", rs.getString("nome"));
						curso.put("resumo", rs.getString("resumo"));
						curso.put("vagas", rs.getInt("vagas"));
						curso.put("cargaHoraria", rs.getInt("carga_horaria"));
						curso.put("dataInicio", rs.getDate("data_inicio").toLocalDate()
								.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
						curso.put("dataTermino", rs.getDate("data_Termino").toLocalDate());
						curso.put("dias", rs.getString("dias"));
						curso.put("horarioInicio", rs.getTime("horario_inicio").toLocalTime());
						curso.put("horarioTermino", rs.getTime("horario_termino").toLocalTime());
						curso.put("programa", rs.getString("programa"));
						curso.put("area", rs.getString("area"));
						curso.put("inscritos", rs.getString("inscritos"));
						cursos.add(curso);
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}

				return pebble.render(new ModelAndView(map, "templates/admin.cursos.pebble"));
			}
		});

		Spark.get("/admin/curso/editar/:id", new Route() {

			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();
				int id = req.params("id") == null ? 0 : Integer.parseInt(req.params("id"));

				if (id == 0) {
					resp.status(400);
					return "CÃ³digo do curso não informado";
				}

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "SELECT c.*, a.nome AS area, (SELECT COUNT(*) FROM inscricoes WHERE id_curso = c.id) AS inscritos FROM cursos AS c JOIN areas AS a ON c.id_area = a.id WHERE c.id = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setInt(1, id);
					ResultSet rs = stmt.executeQuery();

					if (rs.next()) {
						Map<String, Object> curso = new HashMap<>();
						curso.put("id", rs.getInt("id"));
						curso.put("nome", rs.getString("nome"));
						curso.put("resumo", rs.getString("resumo"));
						curso.put("vagas", rs.getInt("vagas"));
						curso.put("cargaHoraria", rs.getInt("carga_horaria"));
						curso.put("dataInicio", rs.getDate("data_inicio").toLocalDate());
						curso.put("dataTermino", rs.getDate("data_Termino").toLocalDate());
						curso.put("dias", rs.getString("dias"));
						curso.put("horarioInicio", rs.getTime("horario_inicio").toLocalTime());
						curso.put("horarioTermino", rs.getTime("horario_termino").toLocalTime());
						curso.put("programa", rs.getString("programa"));
						curso.put("area", rs.getString("area"));
						curso.put("temImagem", rs.getString("tipo_imagem") != null);
						curso.put("inscritos", rs.getString("inscritos"));
						map.put("curso", curso);

						rs = con.prepareStatement("SELECT id, nome FROM areas ORDER BY nome").executeQuery();
						List<Map<String, Object>> areas = new ArrayList<>();
						while (rs.next()) {
							Map<String, Object> area = new HashMap<>();
							area.put("id", rs.getInt("id"));
							area.put("nome", rs.getString("nome"));
							areas.add(area);
						}
						map.put("areas", areas);

					} else {
						resp.status(404);
						return "Curso " + id + " não encontrado";
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return pebble.render(new ModelAndView(map, "templates/admin.cursos.editar.pebble"));
			}
		});

		Spark.post("/admin/curso/editar/:id", new Route() {

			public Object handle(Request req, Response resp) throws Exception {

				int id = req.params("id") == null ? 0 : Integer.parseInt(req.params("id"));

				if (id == 0) {
					resp.status(400);
					return "Código do curso não informado";
				}

				Map<String, String> map = new HashMap<>();
				req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement("/temp"));
				String nome = req.queryParams("nome");
				if (nome == null) {
					map.put("erro", "Nome não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				String resumo = req.queryParams("resumo");
				if (resumo == null) {
					map.put("erro", "Resumo não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				int vagas = req.queryMap("vagas").integerValue();
				if (vagas < 1) {
					map.put("erro", "Quantitativo de vagas nãoo informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				int cargaHoraria = req.queryMap("carga_horaria").integerValue();
				if (cargaHoraria < 1) {
					map.put("erro", "Carga horária não informada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				if (req.queryParams("data_inicio") == null) {
					map.put("erro", "Dada de início não informada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				Date dataInicio = ISODateFormat.parse(req.queryParams("data_inicio"));
				if (req.queryParams("data_termino") == null) {
					map.put("erro", "Data de térrmino não informada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				Date dataTermino = ISODateFormat.parse(req.queryParams("data_termino"));
				String dias = String.join(", ", req.queryParamsValues("dias"));
				if (req.queryParams("horario_inicio") == null) {
					map.put("erro", "Horário de início não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				Date horaInicio = ISOTimeFormat.parse(req.queryParams("horario_inicio"));
				if (req.queryParams("horario_termino") == null) {
					map.put("erro", "Horário de término não informado");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}
				Date horaTermino = ISOTimeFormat.parse(req.queryParams("horario_termino"));
				String programa = req.queryParams("programa");
				Part imagem = req.raw().getPart("imagem");
				InputStream imagemInputStream = null;
				String tipoImagem = null;
				if (imagem.getSize() > 0) {
					imagemInputStream = imagem.getInputStream();
					tipoImagem = imagem.getContentType().split("/")[1];
					// Image reader = new Image(imagemInputStream);
					// return reader.getWidth() + "/" + reader.getHeight();
				}

				if (req.queryParams("area") == null) {
					map.put("erro", "Área não selecionada");
					return pebble.render(new ModelAndView(map, "templates/admin.curso.editar.pebble"));
				}

				int idArea = req.queryMap("area").integerValue();

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {
					String sql = "UPDATE cursos "
							+ "SET nome = ?, resumo = ?, vagas = ?, carga_horaria = ?, data_inicio = ?, data_termino = ?, dias = ?, horario_inicio = ?, horario_termino = ?, programa = ?, imagem = ?, tipo_imagem = ?, id_area = ? "
							+ "WHERE id = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setString(1, nome);
					stmt.setString(2, resumo);
					stmt.setInt(3, vagas);
					stmt.setInt(4, cargaHoraria);
					stmt.setDate(5, new java.sql.Date(dataInicio.getTime()));
					stmt.setDate(6, new java.sql.Date(dataTermino.getTime()));
					stmt.setString(7, dias);
					stmt.setTime(8, new java.sql.Time(horaInicio.getTime()));
					stmt.setTime(9, new java.sql.Time(horaTermino.getTime()));

					if (programa == null || programa.isEmpty()) {
						stmt.setNull(10, Types.VARCHAR);
					} else {
						stmt.setString(10, programa);
					}

					if (imagemInputStream == null) {
						stmt.setNull(11, Types.BLOB);
					} else {
						stmt.setBlob(11, imagemInputStream);
					}

					if (tipoImagem == null) {
						stmt.setNull(12, Types.VARCHAR);
					} else {
						stmt.setString(12, tipoImagem);
					}

					stmt.setInt(13, idArea);

					stmt.setInt(14, id);

					stmt.execute();

					resp.redirect("/admin");
					return "OK";
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			}
		});

		Spark.get("/admin/curso/:id", new Route() {

			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();
				int id = req.params("id") == null ? 0 : Integer.parseInt(req.params("id"));

				if (id == 0) {
					resp.status(400);
					return "Código do curso não informado";
				}

				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {

					String sql = "SELECT c.*, a.nome AS area, (SELECT COUNT(*) FROM inscricoes WHERE id_curso = c.id) AS inscritos FROM cursos AS c JOIN areas AS a ON c.id_area = a.id WHERE c.id = ?";
					PreparedStatement stmt = con.prepareStatement(sql);
					stmt.setInt(1, id);
					ResultSet rs = stmt.executeQuery();

					if (rs.next()) {
						Map<String, Object> curso = new HashMap<>();
						curso.put("id", rs.getInt("id"));
						curso.put("nome", rs.getString("nome"));
						curso.put("resumo", rs.getString("resumo"));
						curso.put("vagas", rs.getInt("vagas"));
						curso.put("cargaHoraria", rs.getInt("carga_horaria"));
						curso.put("dataInicio", rs.getDate("data_inicio").toLocalDate());
						curso.put("dataTermino", rs.getDate("data_Termino").toLocalDate());
						curso.put("dias", rs.getString("dias"));
						curso.put("horarioInicio", rs.getTime("horario_inicio").toLocalTime());
						curso.put("horarioTermino", rs.getTime("horario_termino").toLocalTime());
						curso.put("programa", rs.getString("programa"));
						curso.put("area", rs.getString("area"));
						curso.put("temImagem", rs.getString("tipo_imagem") != null);
						curso.put("inscritos", rs.getString("inscritos"));
						map.put("curso", curso);

						stmt = con.prepareStatement(
								"SELECT u.nome AS aluno FROM usuarios u JOIN inscricoes i ON u.id = i.id_usuario WHERE i.id_curso = ?");
						stmt.setInt(1, id);
						rs = stmt.executeQuery();
						List<String> alunos = new ArrayList<>();
						while (rs.next()) {
							alunos.add(rs.getString("aluno"));
						}
						map.put("alunos", alunos);

					} else {
						resp.status(404);
						return "Curso " + id + " não encontrado";
					}

				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				return pebble.render(new ModelAndView(map, "templates/admin.curso.pebble"));
			}
		});
		Spark.get("/area", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, String> map = new HashMap<>();
				return pebble.render(new ModelAndView(map, "templates/area.pebble"));
			}
		});
		Spark.post("/area", new Route() {
			public Object handle(Request req, Response resp) throws Exception {
				Map<String, Object> map = new HashMap<>();

				Areas novaArea = new Areas(req.queryParams("nome"));

				ORMAutoConvention g = new ORMAutoConvention();

				g.insert(novaArea);

				return pebble.render(new ModelAndView(map, "templates/area.pebble"));

//				novaArea.setNome();
//
//				map.put("novaArea", novaArea);
//
//				if (novaArea.getNome().length() < 5 || novaArea.getNome().length() > 20) {
//					map.put("erro", "Nome deve ter entre 5 e 20 caracteres");
//					return pebble.render(new ModelAndView(map, "templates/area.pebble"));
//				}
//
//				try (Connection con = DriverManager.getConnection("jdbc:sqlite:db/mochinho.sqlite")) {
//
//					AreaMapper areamapper = new AreaMapper();
//					areamapper.insert(novaArea);
//
//					resp.redirect("/area");
//					return "OK";
//				} catch (Exception ex) {
//					throw new RuntimeException(ex);
//				}

			}
		});
	}

}
