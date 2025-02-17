package ca.uhn.fhir.jpa.dao.dstu3;

import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.test.BaseJpaDstu3Test;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import org.hl7.fhir.dstu3.model.Organization;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.dstu3.model.Reference;
import org.hl7.fhir.instance.model.api.IIdType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class FhirResourceDaoDstu3ReferentialIntegrityTest extends BaseJpaDstu3Test {

	@AfterEach
	public void afterResetConfig() {
		myStorageSettings.setEnforceReferentialIntegrityOnWrite(new JpaStorageSettings().isEnforceReferentialIntegrityOnWrite());
		myStorageSettings.setEnforceReferentialIntegrityOnDelete(new JpaStorageSettings().isEnforceReferentialIntegrityOnDelete());
	}

	@Test
	public void testCreateUnknownReferenceFail() {

		Patient p = new Patient();
		p.setManagingOrganization(new Reference("Organization/AAA"));
		try {
			myPatientDao.create(p);
			fail();
		} catch (InvalidRequestException e) {
			assertEquals(Msg.code(1094) + "Resource Organization/AAA not found, specified in path: Patient.managingOrganization", e.getMessage());
		}

	}

	@Test
	public void testCreateUnknownReferenceAllow() throws Exception {
		myStorageSettings.setEnforceReferentialIntegrityOnWrite(false);

		Patient p = new Patient();
		p.setManagingOrganization(new Reference("Organization/AAA"));
		IIdType id = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		p = myPatientDao.read(id);
		assertEquals("Organization/AAA", p.getManagingOrganization().getReference());

	}

	@Test
	public void testDeleteFail() throws Exception {
		Organization o = new Organization();
		o.setName("FOO");
		IIdType oid = myOrganizationDao.create(o).getId().toUnqualifiedVersionless();

		Patient p = new Patient();
		p.setManagingOrganization(new Reference(oid));
		IIdType pid = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		try {
			myOrganizationDao.delete(oid);
			fail();
		} catch (ResourceVersionConflictException e) {
			assertEquals(Msg.code(550) + Msg.code(515) + "Unable to delete Organization/" + oid.getIdPart() + " because at least one resource has a reference to this resource. First reference found was resource Patient/" + pid.getIdPart() + " in path Patient.managingOrganization", e.getMessage());
		}

		myPatientDao.delete(pid);
		myOrganizationDao.delete(oid);

	}

	@Test
	public void testDeleteAllow() throws Exception {
		myStorageSettings.setEnforceReferentialIntegrityOnDelete(false);

		Organization o = new Organization();
		o.setName("FOO");
		IIdType oid = myOrganizationDao.create(o).getId().toUnqualifiedVersionless();

		Patient p = new Patient();
		p.setManagingOrganization(new Reference(oid));
		IIdType pid = myPatientDao.create(p).getId().toUnqualifiedVersionless();

		myOrganizationDao.delete(oid);
		myPatientDao.delete(pid);

	}


}
