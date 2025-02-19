package fr.insarennes.resource;
import fr.insarennes.model.*;
import io.swagger.annotations.Api;
import java.net.HttpURLConnection;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Singleton;
import javax.persistence.*;
import javax.print.attribute.standard.Media;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.apache.log4j.BasicConfigurator;

import static javax.swing.text.html.HTML.Tag.TD;

@Singleton // Q: with and without, see 3.4 https://jersey.java.net/documentation/latest/jaxrs-resources.html
@Path("calendar")
@Api(value = "calendar")
public class CalendarResource {
	private static final Logger LOGGER = Logger.getAnonymousLogger();

	// Static blocks are used to parametrized static objects of the class.
	static {
		// Define the level of importance the Logger has to consider.
		// The logged messages with an importance lower than the one defined here will be ignored.
		LOGGER.setLevel(Level.ALL);

		BasicConfigurator.configure();
		org.apache.log4j.Logger.getRootLogger().setLevel(org.apache.log4j.Level.WARN);
	}

	private final EntityManagerFactory emf;
	private final EntityManager em;
	private final Agenda agenda;

	public CalendarResource() {
		super();
		agenda = new Agenda();
		emf = Persistence.createEntityManagerFactory("agendapp");
		em = emf.createEntityManager();

		final EntityTransaction tr = em.getTransaction();

		tr.begin();
		em.persist(agenda);
		tr.commit();

		// You can add here calendar elements to add by default in the database of the application.
		// For instance:
				try {
					// Each time you add an object into the database or modify an object already added into the database,
					// You must surround your code with a em.getTransaction().begin() that identifies the beginning of a transaction
					// and a em.getTransaction().commit() at the end of the transaction to validate it.
					// In case of crashes, you have to surround the code with a try/catch block, where the catch rollbacks the
					// transaction using em.getTransaction().rollback()
					tr.begin();

					Enseignant ens = new Enseignant("Blouin");
                    Enseignant enss = new Enseignant("Bieber");
					Matiere mat = new Matiere("Web", 3);

					em.persist(ens);
                    em.persist(enss);
					em.persist(mat);

					fr.insarennes.model.TD td = new TD(mat, LocalDate.of(2015, Month.JANUARY, 1).atTime(8, 0), ens, Duration.ofHours(2));
					agenda.addCours(td);
					em.persist(td);

                    fr.insarennes.model.TD tdd = new TD(mat, LocalDate.of(2015, Month.JANUARY, 2).atTime(14, 0), enss, Duration.ofHours(3));
                    agenda.addCours(tdd);
                    em.persist(tdd);

					fr.insarennes.model.TD tddd = new TD(mat, LocalDate.of(2015, Month.JANUARY, 2).atTime(10, 0), ens, Duration.ofHours(1));
					agenda.addCours(tddd);
					em.persist(tddd);

                    tr.commit();

					LOGGER.log(Level.INFO, "Added during the creation of the calendar resource:");
					LOGGER.log(Level.INFO, "a Enseignant: " + ens);
					LOGGER.log(Level.INFO, "a Matiere: " + mat);
					LOGGER.log(Level.INFO, "a TD: " + td);
				}catch(final RollbackException | IllegalStateException ex) {
					LOGGER.log(Level.SEVERE, "Crash during the creation of initial data", ex);
					if(tr.isActive()) {
						tr.rollback();
					}
				}
	}


	public void flush() {
		em.clear();
		em.close();
		emf.close();
	}

	//curl -H "Content-Type: application/json" -d '{"name":"blouin"}' -X POST "http://localhost:8080/calendar/ens"
	// To know the XML format, look at the returned XML message.
	@POST
	@Path("ens/{name}")
	@Produces(MediaType.APPLICATION_XML)
	public Response postEnseignant(@PathParam("name") final String name) {
		final EntityTransaction tr = em.getTransaction();
		try {
			final Enseignant ens = new Enseignant(name);
			// begin starts a transaction:
			// https://en.wikibooks.org/wiki/Java_Persistence/Transactions
			tr.begin();
			em.persist(ens);
			tr.commit();
			// return ens;
			return Response.status(Response.Status.OK).entity(ens).build();
		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			// Loggers are widely used to log information about the execution of a program.
			// The classical use is a static final Logger for each class or for the whole application.
			// Here, the first parameter is the level of importance of the message.
			// The second parameter is the message, and the third one is the exception.
			// Various useful methods compose a Logger.
			// By default, when a message is logged it is printed in the console.
			LOGGER.log(Level.SEVERE, "Crash on adding a Enseignant: " + name, ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}

	@GET
	@Path("ens/")
	@Produces(MediaType.APPLICATION_XML)
	public List<Enseignant> getEnseignant() {
		final EntityTransaction tr = em.getTransaction();
		try {
			return em.createNamedQuery("enseignants", Enseignant.class).getResultList();

		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			// Loggers are widely used to log information about the execution of a program.
			// The classical use is a static final Logger for each class or for the whole application.
			// Here, the first parameter is the level of importance of the message.
			// The second parameter is the message, and the third one is the exception.
			// Various useful methods compose a Logger.
			// By default, when a message is logged it is printed in the console.
			LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " , ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}


	// DO NOT USE begin(), commit() or rollback() for the @GET verb.

	// When modifying an object (@PUT verb) DO NOT USE em.persits(obj) again since the object has been already added to the database during its @POST

	// Do not use @Consumes when no data are sent

	// When adding a course (@POST a course), do not forget to add it to the agenda as well:
	// em.persist(c);
	// agenda.addCours(c);

	// When getting the list of courses for a given week, do not use a SQL command but agenda.getCours();

	// When getting the list of courses for a given week, the Cours class already has a function matchesID(int) that checks whether the given ID is used by the course.


	@POST
	@Path("matiere/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response postMatiere(/*@PathParam("id") final int id,*/ @PathParam("name") final String name) {
		final EntityTransaction tr = em.getTransaction();
		try {
			final Matiere matiere = new Matiere(name, 3);
			// begin starts a transaction:
			// https://en.wikibooks.org/wiki/Java_Persistence/Transactions
			tr.begin();
			em.persist(matiere);
			tr.commit();
			return Response.status(Response.Status.OK).entity(matiere).build();
		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			// Loggers are widely used to log information about the execution of a program.
			// The classical use is a static final Logger for each class or for the whole application.
			// Here, the first parameter is the level of importance of the message.
			// The second parameter is the message, and the third one is the exception.
			// Various useful methods compose a Logger.
			// By default, when a message is logged it is printed in the console.
			LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " + name, ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}

	@GET
	@Path("matiere/")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Matiere> getMatiere() {
		final EntityTransaction tr = em.getTransaction();
		try {
			return em.createNamedQuery("matieres", Matiere.class).getResultList();

		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			// Loggers are widely used to log information about the execution of a program.
			// The classical use is a static final Logger for each class or for the whole application.
			// Here, the first parameter is the level of importance of the message.
			// The second parameter is the message, and the third one is the exception.
			// Various useful methods compose a Logger.
			// By default, when a message is logged it is printed in the console.
			LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " , ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}


	@PUT
	@Path("matiere/{id}/{newName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Matiere putMatiere( @PathParam("id") final int id, @PathParam("newName") final String newName) {
		final EntityTransaction tr = em.getTransaction();
		try {

			final Matiere matiere = em.createNamedQuery("getMatieresFromId", Matiere.class).setParameter("id",Integer.valueOf(id)).getSingleResult();
			tr.begin();
//			if (matiere != null){
				matiere.setName(newName);
				tr.commit();
				return matiere;
//			} else {
//				throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST).build());
//			}
		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			// Loggers are widely used to log information about the execution of a program.
			// The classical use is a static final Logger for each class or for the whole application.
			// Here, the first parameter is the level of importance of the message.
			// The second parameter is the message, and the third one is the exception.
			// Various useful methods compose a Logger.
			// By default, when a message is logged it is printed in the console.
			LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " , ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}

	@GET
	@Path("matiere/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Matiere getMatieresFromName( @PathParam("name") final String name) {
		final EntityTransaction tr = em.getTransaction();
		try {
			return em.createQuery("SELECT m FROM Matiere m WHERE m.name = :name", Matiere.class).setParameter("name", name).getSingleResult();

		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			// Loggers are widely used to log information about the execution of a program.
			// The classical use is a static final Logger for each class or for the whole application.
			// Here, the first parameter is the level of importance of the message.
			// The second parameter is the message, and the third one is the exception.
			// Various useful methods compose a Logger.
			// By default, when a message is logged it is printed in the console.
			LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " , ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}

	@DELETE
	@Path("matiere/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteMatiereFromId( @PathParam("id") final int id) {
		final EntityTransaction tr = em.getTransaction();
		try {
			Matiere matiere = em.find(Matiere.class,id);
			tr.begin();
			em.remove(matiere);
			tr.commit();
			return Response.status(Response.Status.OK).entity(matiere).build();

		}catch(final RollbackException | IllegalStateException ex) {
			// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
			if(tr.isActive()) {
				tr.rollback();
			}
			LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " , ex);
			// A Web exception is then thrown.
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
		}catch(final NullPointerException ex) {
			throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "The name is not correct").build());
		}
	}

	@POST
	@Path("cours")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Cours postCours(Cours cours){
		final EntityTransaction tr = em.getTransaction();
		try {
			tr.begin();
			em.persist(cours);
			tr.commit();
			return cours;
		} catch(final RollbackException | IllegalStateException ex) {
		// If an exception occurs after a begin and before the commit, the transaction has to be rollbacked.
		if(tr.isActive()) {
			tr.rollback();
		}
		LOGGER.log(Level.SEVERE, "Crash on adding a Matiere: " , ex);
		// A Web exception is then thrown.
		throw new WebApplicationException(Response.status(HttpURLConnection.HTTP_BAD_REQUEST, "Cannot persist").build());
	}
	}

	@GET
	@Path("cours/{week}/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getRessourceWeek(@PathParam("week") final int week, @PathParam("id") final int id){
		final EntityTransaction tr = em.getTransaction();
		List<Cours> tmpList =  em.createQuery("SELECT c FROM Cours c", Cours.class).getResultList();
		List<Cours> result = new ArrayList<Cours>();
		for(Cours cours: tmpList){
			if(cours.getHoraire().get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()) == week) result.add(cours);
			else if(cours.matchesID(id)) result.add(cours);
			else if(cours.getEns().getId()==id) result.add(cours);
			else if(cours.getMatiere().getId()==id)result.add(cours);
		}

		return Response.status(Response.Status.OK).entity(result.toArray(new Cours[result.size()])).build();


	}
}

