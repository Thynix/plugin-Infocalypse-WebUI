package org.freenetproject.plugin.dvcs_webui.main;

import freenet.client.HighLevelSimpleClient;
import freenet.clients.http.SessionManager;
import freenet.clients.http.Toadlet;
import freenet.clients.http.ToadletContainer;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginFCP;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginReplySender;
import freenet.pluginmanager.PluginRespirator;
import freenet.support.SimpleFieldSet;
import freenet.support.api.Bucket;
import org.freenetproject.plugin.dvcs_webui.ui.fcp.FCPHandler;
import org.freenetproject.plugin.dvcs_webui.ui.web.Homepage;
import org.freenetproject.plugin.dvcs_webui.ui.web.RepositoryList;

import java.util.ArrayList;

/**
 * Registers the plugin with the Freenet node: pages and for FCP.
 * TODO: Is there no way to have only a different class implement FredPluginFCP, or must it be this one?
 */
public class Plugin implements FredPlugin, FredPluginThreadless, FredPluginVersioned, FredPluginFCP {

	private PluginRespirator pluginRespirator;
	private ToadletContainer tc;

	private final ArrayList<Toadlet> toadlets;
	private FCPHandler fcpHandler;

	private static final String MENU_NAME = "Menu";

	public static final String ENCODING = "UTF-8";

	/**
	 * Length in characters of a WoT identity ID, which is also an SSK public key hash.
	 */
	public static final int IDENTITY_ID_LENGTH = 43;

	/**
	 * Maximum length of an identifier for a WoT identity. This can consist of a nickname,
	 * which is up to 30 characters, an '@', and an identity ID.
	 */
	public static final int MAX_IDENTIFIER_LENGTH = 30 + 1 + IDENTITY_ID_LENGTH;

	/**
	 * Cookie namespace for sessions. Used to find WoT logins.
	 */
	private static final String SESSION_NAMESPACE = "WebOfTrust";

	public Plugin() {
		/*
		 * If micro-optimizing this could be set to an initial capacity. However, note the ArrayList documentation:
		 *
		 * "The details of the growth policy are not specified beyond the fact that adding an element has constant
		 * amortized time cost."
		 */
		toadlets = new ArrayList<Toadlet>();
	}

	public String getVersion() {
		return "0.1-SNAPSHOT";
	}

	@Override
	public void runPlugin(PluginRespirator pr) {
		pluginRespirator = pr;
		tc = pluginRespirator.getToadletContainer();
		final HighLevelSimpleClient hLSimpleClient = pr.getHLSimpleClient();

		L10n l10n = new L10n();
		WoTConnector woTConnector = new WoTConnector(pr);
		SessionManager sessionManager = pr.getSessionManager(SESSION_NAMESPACE);

		fcpHandler = new FCPHandler(pr.getNode().random);
		Homepage homepage = new Homepage(hLSimpleClient, l10n, fcpHandler, woTConnector, sessionManager);
		RepositoryList repositoryList = new RepositoryList(hLSimpleClient, l10n, fcpHandler, sessionManager);

		// TODO: Reduce duplication by having a wrapper provide the name and title keys. (getName()?)
		toadlets.add(homepage);
		toadlets.add(repositoryList);

		pluginRespirator.getPageMaker().addNavigationCategory(homepage.path(), MENU_NAME, MENU_NAME, l10n);
		tc.register(homepage, MENU_NAME, homepage.path(), true, MENU_NAME, MENU_NAME, false, homepage);
		tc.register(repositoryList, MENU_NAME, repositoryList.path(), true, "ListRepositories", "ListRepositories",
				false, repositoryList);
	}

	@Override
	public void terminate() {
		for (Toadlet toadlet : toadlets) {
			tc.unregister(toadlet);
		}

		pluginRespirator.getPageMaker().removeNavigationCategory(MENU_NAME);
	}

	@Override
	public void handle(PluginReplySender replysender, SimpleFieldSet params, Bucket data, int accesstype) {
		fcpHandler.handle(replysender, params, data, accesstype);
	}
}
