package org.molgenis.security;

import org.molgenis.data.DataService;
import org.molgenis.omx.auth.GroupAuthority;
import org.molgenis.omx.auth.MolgenisGroup;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.auth.UserAuthority;
import org.molgenis.security.account.AccountService;
import org.molgenis.security.core.utils.SecurityUtils;
import org.molgenis.security.runas.RunAsSystem;
import org.molgenis.security.user.UserAccountController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MolgenisSecurityWebAppDatabasePopulatorServiceImpl implements
		MolgenisSecurityWebAppDatabasePopulatorService
{
	private static final String USERNAME_ADMIN = "admin";

	@Value("${admin.password:@null}")
	private String adminPassword;
	@Value("${admin.email:molgenis+admin@gmail.com}")
	private String adminEmail;
	@Value("${anonymous.email:molgenis+anonymous@gmail.com}")
	private String anonymousEmail;

	private MolgenisUser userAdmin;
	private MolgenisUser anonymousUser;
	private MolgenisGroup allUsersGroup;

	@Override
	@Transactional
	@RunAsSystem
	public void populateDatabase(DataService dataService, String homeControllerId)
	{
		if (adminPassword == null) throw new RuntimeException(
				"please configure the admin.password property in your molgenis-server.properties");

		// create admin user
		userAdmin = new MolgenisUser();
		userAdmin.setUsername(USERNAME_ADMIN);
		userAdmin.setPassword(adminPassword);
		userAdmin.setEmail(adminEmail);
		userAdmin.setActive(true);
		userAdmin.setSuperuser(true);
		userAdmin.setChangePassword(false);
		dataService.add(MolgenisUser.ENTITY_NAME, userAdmin);

		// create anonymous user
		anonymousUser = new MolgenisUser();
		anonymousUser.setUsername(SecurityUtils.ANONYMOUS_USERNAME);
		anonymousUser.setPassword(SecurityUtils.ANONYMOUS_USERNAME);
		anonymousUser.setEmail(anonymousEmail);
		anonymousUser.setActive(true);
		anonymousUser.setSuperuser(false);
		anonymousUser.setChangePassword(false);
		dataService.add(MolgenisUser.ENTITY_NAME, anonymousUser);

		// set anonymous role for anonymous user
		UserAuthority anonymousAuthority = new UserAuthority();
		anonymousAuthority.setMolgenisUser(anonymousUser);
		anonymousAuthority.setRole(SecurityUtils.AUTHORITY_ANONYMOUS);
		dataService.add(UserAuthority.ENTITY_NAME, anonymousAuthority);

		// create all users group
		allUsersGroup = new MolgenisGroup();
		allUsersGroup.setName(AccountService.ALL_USER_GROUP);
		dataService.add(MolgenisGroup.ENTITY_NAME, allUsersGroup);

		// allow all users to see the home plugin
		GroupAuthority usersGroupHomeAuthority = new GroupAuthority();
		usersGroupHomeAuthority.setMolgenisGroup(allUsersGroup);
		usersGroupHomeAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_READ_PREFIX + homeControllerId.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, usersGroupHomeAuthority);

		// allow all users to update their profile
		GroupAuthority usersGroupUserAccountAuthority = new GroupAuthority();
		usersGroupUserAccountAuthority.setMolgenisGroup(allUsersGroup);
		usersGroupUserAccountAuthority.setRole(SecurityUtils.AUTHORITY_PLUGIN_WRITE_PREFIX
				+ UserAccountController.ID.toUpperCase());
		dataService.add(GroupAuthority.ENTITY_NAME, usersGroupUserAccountAuthority);
	}

	@Override
	public MolgenisUser getAnonymousUser()
	{
		return anonymousUser;
	}

	@Override
	public MolgenisUser getUserAdmin()
	{
		return userAdmin;
	}

	@Override
	public MolgenisGroup getAllUsersGroup()
	{
		return allUsersGroup;
	}
}