package com.booleanuk.api.requests.salary;

import javax.sql.DataSource;
import org.postgresql.ds.PGSimpleDataSource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SalaryRepository {
    DataSource datasource;
    String dbUser;
    String dbURL;
    String dbPassword;
    String dbDatabase;
    Connection connection;

    public SalaryRepository() throws SQLException {
        this.getDatabaseCredentials();
        this.datasource = this.createDataSource();
        this.connection = this.datasource.getConnection();
    }

    private void getDatabaseCredentials() {
        try (InputStream input = new FileInputStream("src/main/resources/config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            this.dbUser = prop.getProperty("db.user");
            this.dbURL = prop.getProperty("db.url");
            this.dbPassword = prop.getProperty("db.password");
            this.dbDatabase = prop.getProperty("db.database");
        } catch(Exception e) {
            System.out.println("Unable to read config.properties file: " + e);
        }
    }

    private DataSource createDataSource() {
        // The url specifies the address of our database along with username and password credentials
        // you should replace these with your own username and password
        final String url =
                "jdbc:postgresql://" + this.dbURL
                        + ":5432/" + this.dbDatabase
                        + "?user=" + this.dbUser
                        +"&password=" + this.dbPassword;

        final PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setUrl(url);
        return dataSource;
    }

    public Salary createSalary(Salary salary) throws SQLException {
        String insertSQL = "INSERT INTO Salaries(grade, minSalary, maxSalary) VALUES(?, ?, ?)";

        PreparedStatement statement = this.connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);

        statement.setString(1, salary.getGrade());
        statement.setInt(2, salary.getMinSalary());
        statement.setInt(3, salary.getMaxSalary());

        int rowsAffected = statement.executeUpdate();

        long newId = 0;

        if (rowsAffected > 0) {

            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    newId = rs.getLong(1);  //Ligger i column 1 i databasen. Hämtas eftersom det skapas automatiskt med SERIAL
                }
            } catch (Exception e) {
                System.out.println("Newly created salary's SERIAL id could not be retrieved from database: " + e);
            }

            salary.setId(newId);

        }
        else {
            salary = null;
        }

        return salary;
    }

    public List<Salary> getSalaries() throws SQLException  {
        List<Salary> allSalaries = new ArrayList<>();

        PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM Salaries");

        ResultSet results = statement.executeQuery();

        while (results.next()) {
            Salary salary = new Salary(
                    results.getLong("id"),
                    results.getString("grade"),
                    results.getInt("minSalary"),
                    results.getInt("maxSalary"));
            allSalaries.add(salary);
        }

        return allSalaries;
    }

    public Salary getSpecificSalary(long id) throws SQLException {
        PreparedStatement statement = this.connection.prepareStatement("SELECT * FROM Salaries WHERE id = ?");

        // Choose set**** matching the datatype of the missing element
        statement.setLong(1, id);

        ResultSet results = statement.executeQuery();

        Salary salary = null;

        if (results.next()) {
            salary = new Salary(
                    results.getLong("id"),
                    results.getString("grade"),
                    results.getInt("minSalary"),
                    results.getInt("maxSalary"));
        }

        return salary;
    }

    public Salary updateSalary(long id, Salary salary) throws SQLException {
        String SQL = "UPDATE Employees " +
                "SET grade = ? ," +
                "minSalary = ? ," +
                "maxSalary = ? ," +
                "WHERE id = ? ";

        PreparedStatement statement = this.connection.prepareStatement(SQL);

        statement.setString(1, salary.getGrade());
        statement.setInt(2, salary.getMinSalary());
        statement.setInt(3, salary.getMaxSalary());
        statement.setLong(5, id);

        int rowsAffected = statement.executeUpdate();

        Salary updatedSalary = null;

        if (rowsAffected > 0) {
            updatedSalary = this.getSpecificSalary(id);
        }

        return updatedSalary;
    }

    public Salary deleteSalary(long id) throws SQLException {
        String SQL = "DELETE FROM Salaries WHERE id = ?";

        PreparedStatement statement = this.connection.prepareStatement(SQL);

        // Get the employee before we delete them
        Salary deletedSalary = null;

        deletedSalary = this.getSpecificSalary(id);

        statement.setLong(1, id);

        int rowsAffected = statement.executeUpdate();

        if (rowsAffected == 0) {
            //Reset the employee we're deleting if we didn't delete them
            deletedSalary = null;
        }

        return deletedSalary;
    }
}
