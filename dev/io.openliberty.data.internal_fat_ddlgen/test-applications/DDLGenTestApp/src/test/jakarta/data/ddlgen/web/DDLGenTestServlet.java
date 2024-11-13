/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package test.jakarta.data.ddlgen.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/*")
public class DDLGenTestServlet extends FATServlet {

    @Inject
    Parts parts;

    @Inject
    Cars cars;

    @Inject
    Trucks trucks;

    /**
     * Executes the DDL in the database as a database admin.
     * This method is intentionally not annotated with @Test.
     * The test bucket must arrange for it to run before other test
     * as part of setup.
     */
    public void executeDDL(HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<String> files = Arrays.asList(Objects.requireNonNull(request.getParameter("scripts")).split(","));
        String usingDataSource = Objects.requireNonNull(request.getParameter("usingDataSource"));
        String preamble = Objects.requireNonNull(request.getParameter("preamble")).replace("]", " ");

        DataSource admin = InitialContext.doLookup(usingDataSource);

        for (String file : files) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                System.out.println("Execute DDL file: " + file);
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.equals("EXIT;"))
                        continue;

                    try (Connection con = admin.getConnection(); Statement stmt = con.createStatement()) {
                        if (!preamble.isBlank()) {
                            System.out.println("  Execute Preamble: " + preamble);
                            stmt.executeUpdate(preamble);
                        }
                        System.out.println("  Execute SQL Statement: " + line);
                        stmt.execute(line);
                    }
                }
            }
        }

    }

    /**
     * Attempt to insert, find, and delete a row in the Cars table
     * which was created via execution of generated ddl files
     */
    @Test
    public void testSaveToDefaultDatabase() {
        assertEquals("Table car should not have any starting values", 0, cars.findAll().count());

        String id = cars.save(Car.of("1234", "Honda", "Civic", 2014, 89452, 7500)).vin;

        Car result = cars.findById(id).orElseThrow();
        assertEquals("Honda", result.make);
        assertEquals("Civic", result.model);
        assertEquals(2014, result.modelYear);
        assertEquals(89452, result.odometer);
        assertEquals(7500, result.price, 0.1);

        cars.delete(result);
    }

    /**
     * Attempt to insert, find, and delete a row in the Trucks table
     * which was created via execution of generated ddl files
     *
     * @throws NamingException
     */
    @Test
    public void testSaveToDatasourceId() throws Exception {
        assertEquals("Table truck should not have any starting values", 0, trucks.findAll().count());

        String id = trucks.save(Truck.of("1234", "Honda", "Ridgeline", 2025, 10, 40150, 63.6)).vin;

        //Ensure no data was not put into the automobile table
        DataSource testDataSource = InitialContext.doLookup("jdbc/TestDataSource");
        try (Connection con = testDataSource.getConnection(); Statement stmt = con.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM Automobile")) {
                assertFalse("Truck element should have been saved to Truck table, not Automobile", rs.next()); //No data in table
            }
        }

        Truck result = trucks.findById(id).orElseThrow();
        assertEquals("Honda", result.make);
        assertEquals("Ridgeline", result.model);
        assertEquals(2025, result.modelYear);
        assertEquals(10, result.odometer);
        assertEquals(40150, result.price, 0.1);
        assertEquals(63.6, result.bedLength, 0.01);

        trucks.delete(result);
    }

    /**
     * Modify and query for a record entity based on its composite id using
     * BasicRepository.save and BasicRepository.findById.
     */
    @Test
    public void testSaveAndFindByEmbeddedId() {
        Part part1 = parts.save(Part.of("EI3155-T", "IBM", "First Part", 10.99f));
        Part part2 = parts.save(Part.of("EI2303-W", "IBM", "Second Part", 8.99f));
        Part part3 = parts.save(Part.of("EI2303-W", "Acme", "Third Part", 9.99f));

        int part2InitialModCount = part2.version();

        part2 = parts.save(new Part(//
                        part2.id(), //
                        part2.name(), //
                        part2.price() + 0.50f, //
                        part2InitialModCount));

        // expect automatic update to version:
        assertEquals(part2InitialModCount + 1, part2.version());

        part2 = parts.findById(new Part.Identifier("EI2303-W", "IBM"))
                        .orElseThrow();
        assertEquals("Second Part", part2.name());
        assertEquals(9.49f, part2.price(), 0.001f);
        assertEquals(new Part.Identifier("EI2303-W", "IBM"), part2.id());
        assertEquals(part2InitialModCount + 1, part2.version());

        Part.Identifier nonmatching = new Part.Identifier("EI3155-T", "Acme");
        assertEquals(false, parts.findById(nonmatching).isPresent());

        assertEquals(true, parts.findById(part1.id()).isPresent());

        parts.deleteById(part1.id());

        assertEquals(false, parts.findById(part1.id()).isPresent());

        parts.deleteAll(List.of(part2, part3));
    }

}
