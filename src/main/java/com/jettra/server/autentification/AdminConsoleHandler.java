package com.jettra.server.autentification;

import com.jettra.server.autentification.entity.JCredential;
import com.jettra.server.autentification.entity.JRole;
import com.jettra.server.autentification.entity.JUser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.jettra.server.autentification.repository.*;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class AdminConsoleHandler implements HttpHandler {

    private final JRoleRepository roleRepo = new JRoleRepositoryImpl();
    private final JUserRepository userRepo = new JUserRepositoryImpl();
    private final JCredentialRepository credRepo = new JCredentialRepositoryImpl();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();

        // Remove trailing slash if any (except for root path)
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }

        try {
            if ("GET".equalsIgnoreCase(method)) {
                if (path.endsWith("/securitydb/admin")) {
                    serveHtml(exchange);
                } else if (path.endsWith("/securitydb/admin/data")) {
                    serveData(exchange);
                } else {
                    sendError(exchange, 404, "Not Found");
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                if (path.endsWith("/securitydb/admin/add-user")) {
                    handleAddUser(exchange);
                } else if (path.endsWith("/securitydb/admin/add-credential")) {
                    handleAddCredential(exchange);
                } else if (path.endsWith("/securitydb/admin/delete-user")) {
                    handleDeleteUser(exchange);
                } else if (path.endsWith("/securitydb/admin/delete-credential")) {
                    handleDeleteCredential(exchange);
                } else {
                    sendError(exchange, 404, "Not Found");
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendError(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }

    private void serveHtml(HttpExchange exchange) throws IOException {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"utf-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "  <meta name=\"description\" content=\"JettraSecurityDB Admin Console\" />\n" +
                "  <title>JettraSecurityDB Admin Console</title>\n" +
                "  <link href=\"https://fonts.googleapis.com/css2?family=Outfit:wght@300;400;500;600;700&display=swap\" rel=\"stylesheet\">\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --bg-color: #0b0f19;\n" +
                "      --card-bg: rgba(17, 24, 39, 0.7);\n" +
                "      --card-border: rgba(255, 255, 255, 0.08);\n" +
                "      --text-color: #f3f4f6;\n" +
                "      --text-muted: #9ca3af;\n" +
                "      --primary: #6366f1;\n" +
                "      --primary-hover: #4f46e5;\n" +
                "      --secondary: #06b6d4;\n" +
                "      --success: #10b981;\n" +
                "      --danger: #f43f5e;\n" +
                "      --sidebar-width: 260px;\n" +
                "    }\n" +
                "    \n" +
                "    * {\n" +
                "      box-sizing: border-box;\n" +
                "      margin: 0;\n" +
                "      padding: 0;\n" +
                "      font-family: 'Outfit', sans-serif;\n" +
                "      transition: all 0.25s ease;\n" +
                "    }\n" +
                "    \n" +
                "    body {\n" +
                "      background-color: var(--bg-color);\n" +
                "      color: var(--text-color);\n" +
                "      min-height: 100vh;\n" +
                "      overflow-x: hidden;\n" +
                "      display: flex;\n" +
                "      background-image:\n" +
                "        radial-gradient(at 10% 20%, rgba(99, 102, 241, 0.15) 0px, transparent 50%),\n" +
                "        radial-gradient(at 90% 80%, rgba(6, 182, 212, 0.15) 0px, transparent 50%);\n" +
                "      background-attachment: fixed;\n" +
                "    }\n" +
                "    \n" +
                "    /* Sidebar */\n" +
                "    aside {\n" +
                "      width: var(--sidebar-width);\n" +
                "      background: rgba(10, 15, 30, 0.8);\n" +
                "      backdrop-filter: blur(20px);\n" +
                "      border-right: 1px solid var(--card-border);\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      padding: 2rem 1.5rem;\n" +
                "      position: fixed;\n" +
                "      height: 100vh;\n" +
                "      z-index: 10;\n" +
                "    }\n" +
                "    \n" +
                "    .logo-container {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 0.75rem;\n" +
                "      margin-bottom: 3rem;\n" +
                "    }\n" +
                "    \n" +
                "    .logo-icon {\n" +
                "      width: 40px;\n" +
                "      height: 40px;\n" +
                "      background: linear-gradient(135deg, var(--primary), var(--secondary));\n" +
                "      border-radius: 12px;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      font-weight: 700;\n" +
                "      color: white;\n" +
                "      box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);\n" +
                "    }\n" +
                "    \n" +
                "    .logo-text {\n" +
                "      font-size: 1.25rem;\n" +
                "      font-weight: 700;\n" +
                "      letter-spacing: 1px;\n" +
                "      background: linear-gradient(to right, #ffffff, #9ca3af);\n" +
                "      -webkit-background-clip: text;\n" +
                "      -webkit-text-fill-color: transparent;\n" +
                "    }\n" +
                "    \n" +
                "    nav {\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      gap: 0.5rem;\n" +
                "    }\n" +
                "    \n" +
                "    .nav-item {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 1rem;\n" +
                "      padding: 0.85rem 1.25rem;\n" +
                "      border-radius: 12px;\n" +
                "      color: var(--text-muted);\n" +
                "      text-decoration: none;\n" +
                "      font-weight: 500;\n" +
                "      cursor: pointer;\n" +
                "    }\n" +
                "    \n" +
                "    .nav-item:hover, .nav-item.active {\n" +
                "      color: var(--text-color);\n" +
                "      background: rgba(255, 255, 255, 0.05);\n" +
                "    }\n" +
                "    \n" +
                "    .nav-item.active {\n" +
                "      border-left: 3px solid var(--primary);\n" +
                "      background: rgba(99, 102, 241, 0.1);\n" +
                "    }\n" +
                "    \n" +
                "    /* Main Content Area */\n" +
                "    main {\n" +
                "      margin-left: var(--sidebar-width);\n" +
                "      flex-grow: 1;\n" +
                "      padding: 3rem;\n" +
                "      max-width: 1200px;\n" +
                "    }\n" +
                "    \n" +
                "    header {\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "      margin-bottom: 3rem;\n" +
                "    }\n" +
                "    \n" +
                "    h1 {\n" +
                "      font-size: 2.25rem;\n" +
                "      font-weight: 700;\n" +
                "      background: linear-gradient(to right, #fff, #9ca3af);\n" +
                "      -webkit-background-clip: text;\n" +
                "      -webkit-text-fill-color: transparent;\n" +
                "    }\n" +
                "    \n" +
                "    .status-badge {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 0.5rem;\n" +
                "      background: rgba(16, 185, 129, 0.1);\n" +
                "      color: var(--success);\n" +
                "      border: 1px solid rgba(16, 185, 129, 0.2);\n" +
                "      padding: 0.5rem 1rem;\n" +
                "      border-radius: 9999px;\n" +
                "      font-size: 0.85rem;\n" +
                "      font-weight: 600;\n" +
                "    }\n" +
                "    \n" +
                "    .status-dot {\n" +
                "      width: 8px;\n" +
                "      height: 8px;\n" +
                "      background-color: var(--success);\n" +
                "      border-radius: 50%;\n" +
                "      box-shadow: 0 0 10px var(--success);\n" +
                "      animation: pulse 2s infinite;\n" +
                "    }\n" +
                "    \n" +
                "    @keyframes pulse {\n" +
                "      0% { transform: scale(0.9); opacity: 0.8; }\n" +
                "      50% { transform: scale(1.1); opacity: 1; }\n" +
                "      100% { transform: scale(0.9); opacity: 0.8; }\n" +
                "    }\n" +
                "    \n" +
                "    /* Dashboard Stats Grid */\n" +
                "    .stats-grid {\n" +
                "      display: grid;\n" +
                "      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));\n" +
                "      gap: 1.5rem;\n" +
                "      margin-bottom: 3rem;\n" +
                "    }\n" +
                "    \n" +
                "    .stat-card {\n" +
                "      background: var(--card-bg);\n" +
                "      backdrop-filter: blur(12px);\n" +
                "      border: 1px solid var(--card-border);\n" +
                "      border-radius: 16px;\n" +
                "      padding: 1.5rem;\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      gap: 0.5rem;\n" +
                "      position: relative;\n" +
                "      overflow: hidden;\n" +
                "    }\n" +
                "    \n" +
                "    .stat-card::before {\n" +
                "      content: '';\n" +
                "      position: absolute;\n" +
                "      top: 0;\n" +
                "      left: 0;\n" +
                "      width: 100%;\n" +
                "      height: 4px;\n" +
                "      background: linear-gradient(90deg, var(--primary), var(--secondary));\n" +
                "    }\n" +
                "    \n" +
                "    .stat-label {\n" +
                "      font-size: 0.875rem;\n" +
                "      color: var(--text-muted);\n" +
                "      font-weight: 500;\n" +
                "    }\n" +
                "    \n" +
                "    .stat-val {\n" +
                "      font-size: 2rem;\n" +
                "      font-weight: 700;\n" +
                "    }\n" +
                "    \n" +
                "    /* Tabs Container */\n" +
                "    .tab-content {\n" +
                "      display: none;\n" +
                "      animation: fadeInUp 0.4s ease forwards;\n" +
                "    }\n" +
                "    \n" +
                "    .tab-content.active {\n" +
                "      display: block;\n" +
                "    }\n" +
                "    \n" +
                "    @keyframes fadeInUp {\n" +
                "      from { opacity: 0; transform: translateY(15px); }\n" +
                "      to { opacity: 1; transform: translateY(0); }\n" +
                "    }\n" +
                "    \n" +
                "    /* Tables & Forms */\n" +
                "    .card {\n" +
                "      background: var(--card-bg);\n" +
                "      backdrop-filter: blur(12px);\n" +
                "      border: 1px solid var(--card-border);\n" +
                "      border-radius: 20px;\n" +
                "      padding: 2.25rem;\n" +
                "      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);\n" +
                "    }\n" +
                "    \n" +
                "    .card-title {\n" +
                "      font-size: 1.5rem;\n" +
                "      font-weight: 600;\n" +
                "      margin-bottom: 1.5rem;\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "    }\n" +
                "    \n" +
                "    table {\n" +
                "      width: 100%;\n" +
                "      border-collapse: collapse;\n" +
                "      margin-top: 1rem;\n" +
                "      text-align: left;\n" +
                "    }\n" +
                "    \n" +
                "    th, td {\n" +
                "      padding: 1rem 1.25rem;\n" +
                "      border-bottom: 1px solid rgba(255, 255, 255, 0.05);\n" +
                "    }\n" +
                "    \n" +
                "    th {\n" +
                "      color: var(--text-muted);\n" +
                "      font-weight: 600;\n" +
                "      font-size: 0.85rem;\n" +
                "      text-transform: uppercase;\n" +
                "      letter-spacing: 0.5px;\n" +
                "    }\n" +
                "    \n" +
                "    tr:hover {\n" +
                "      background: rgba(255, 255, 255, 0.02);\n" +
                "    }\n" +
                "    \n" +
                "    /* Forms */\n" +
                "    .form-grid {\n" +
                "      display: grid;\n" +
                "      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));\n" +
                "      gap: 1.5rem;\n" +
                "      margin-bottom: 2rem;\n" +
                "    }\n" +
                "    \n" +
                "    .input-group {\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      gap: 0.5rem;\n" +
                "    }\n" +
                "    \n" +
                "    label {\n" +
                "      font-size: 0.875rem;\n" +
                "      font-weight: 500;\n" +
                "      color: var(--text-muted);\n" +
                "    }\n" +
                "    \n" +
                "    input[type=\"text\"], input[type=\"email\"], input[type=\"password\"], select {\n" +
                "      background: rgba(255, 255, 255, 0.03);\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.1);\n" +
                "      border-radius: 10px;\n" +
                "      padding: 0.85rem 1rem;\n" +
                "      color: var(--text-color);\n" +
                "      outline: none;\n" +
                "      font-size: 0.95rem;\n" +
                "    }\n" +
                "    \n" +
                "    input:focus, select:focus {\n" +
                "      border-color: var(--primary);\n" +
                "      background: rgba(255, 255, 255, 0.06);\n" +
                "      box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.25);\n" +
                "    }\n" +
                "    \n" +
                "    .checkbox-group {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 0.5rem;\n" +
                "      margin-top: 0.5rem;\n" +
                "    }\n" +
                "    \n" +
                "    .checkbox-group input[type=\"checkbox\"] {\n" +
                "      width: 18px;\n" +
                "      height: 18px;\n" +
                "      accent-color: var(--primary);\n" +
                "    }\n" +
                "    \n" +
                "    .roles-checkboxes {\n" +
                "      display: flex;\n" +
                "      flex-wrap: wrap;\n" +
                "      gap: 1.25rem;\n" +
                "      margin-top: 0.5rem;\n" +
                "      background: rgba(255, 255, 255, 0.02);\n" +
                "      padding: 1rem;\n" +
                "      border-radius: 10px;\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.05);\n" +
                "    }\n" +
                "    \n" +
                "    .badge {\n" +
                "      padding: 0.25rem 0.6rem;\n" +
                "      border-radius: 9999px;\n" +
                "      font-size: 0.75rem;\n" +
                "      font-weight: 600;\n" +
                "      display: inline-block;\n" +
                "    }\n" +
                "    \n" +
                "    .badge-primary {\n" +
                "      background: rgba(99, 102, 241, 0.15);\n" +
                "      color: var(--primary);\n" +
                "      border: 1px solid rgba(99, 102, 241, 0.3);\n" +
                "    }\n" +
                "    \n" +
                "    .badge-success {\n" +
                "      background: rgba(16, 185, 129, 0.15);\n" +
                "      color: var(--success);\n" +
                "      border: 1px solid rgba(16, 185, 129, 0.3);\n" +
                "    }\n" +
                "    \n" +
                "    .badge-danger {\n" +
                "      background: rgba(244, 63, 94, 0.15);\n" +
                "      color: var(--danger);\n" +
                "      border: 1px solid rgba(244, 63, 94, 0.3);\n" +
                "    }\n" +
                "    \n" +
                "    .btn {\n" +
                "      background: linear-gradient(135deg, var(--primary), var(--primary-hover));\n" +
                "      border: none;\n" +
                "      color: white;\n" +
                "      padding: 0.85rem 1.75rem;\n" +
                "      font-weight: 600;\n" +
                "      border-radius: 10px;\n" +
                "      cursor: pointer;\n" +
                "      box-shadow: 0 4px 15px rgba(99, 102, 241, 0.3);\n" +
                "      display: inline-flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      gap: 0.5rem;\n" +
                "    }\n" +
                "    \n" +
                "    .btn:hover {\n" +
                "      transform: translateY(-2px);\n" +
                "      box-shadow: 0 6px 20px rgba(99, 102, 241, 0.45);\n" +
                "    }\n" +
                "    \n" +
                "    .btn-danger {\n" +
                "      background: linear-gradient(135deg, var(--danger), #e11d48);\n" +
                "      box-shadow: 0 4px 15px rgba(244, 63, 94, 0.3);\n" +
                "    }\n" +
                "    \n" +
                "    .btn-danger:hover {\n" +
                "      box-shadow: 0 6px 20px rgba(244, 63, 94, 0.45);\n" +
                "    }\n" +
                "    \n" +
                "    .btn-secondary {\n" +
                "      background: rgba(255, 255, 255, 0.05);\n" +
                "      color: var(--text-color);\n" +
                "      border: 1px solid rgba(255, 255, 255, 0.1);\n" +
                "      box-shadow: none;\n" +
                "    }\n" +
                "    \n" +
                "    .btn-secondary:hover {\n" +
                "      background: rgba(255, 255, 255, 0.1);\n" +
                "      box-shadow: none;\n" +
                "    }\n" +
                "    \n" +
                "    /* Toasts alert system */\n" +
                "    #toast-container {\n" +
                "      position: fixed;\n" +
                "      bottom: 2rem;\n" +
                "      right: 2rem;\n" +
                "      z-index: 100;\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      gap: 0.75rem;\n" +
                "    }\n" +
                "    \n" +
                "    .toast {\n" +
                "      background: rgba(17, 24, 39, 0.9);\n" +
                "      backdrop-filter: blur(12px);\n" +
                "      border: 1px solid var(--card-border);\n" +
                "      border-radius: 12px;\n" +
                "      padding: 1rem 1.5rem;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 1rem;\n" +
                "      box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);\n" +
                "      transform: translateY(50px);\n" +
                "      opacity: 0;\n" +
                "      animation: slideIn 0.3s ease forwards;\n" +
                "    }\n" +
                "    \n" +
                "    .toast.success {\n" +
                "      border-left: 4px solid var(--success);\n" +
                "    }\n" +
                "    \n" +
                "    .toast.error {\n" +
                "      border-left: 4px solid var(--danger);\n" +
                "    }\n" +
                "    \n" +
                "    @keyframes slideIn {\n" +
                "      to { transform: translateY(0); opacity: 1; }\n" +
                "    }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  \n" +
                "  <!-- Sidebar -->\n" +
                "  <aside>\n" +
                "    <div class=\"logo-container\">\n" +
                "      <div class=\"logo-icon\">J</div>\n" +
                "      <span class=\"logo-text\">JettraDB</span>\n" +
                "    </div>\n" +
                "    <nav>\n" +
                "      <div class=\"nav-item active\" onclick=\"showTab('users-tab', this)\">\n" +
                "        <span>Users</span>\n" +
                "      </div>\n" +
                "      <div class=\"nav-item\" onclick=\"showTab('creds-tab', this)\">\n" +
                "        <span>Credentials</span>\n" +
                "      </div>\n" +
                "      <div class=\"nav-item\" onclick=\"showTab('roles-tab', this)\">\n" +
                "        <span>Roles</span>\n" +
                "      </div>\n" +
                "      <div class=\"nav-item\" onclick=\"showTab('add-user-tab', this)\">\n" +
                "        <span>Add User</span>\n" +
                "      </div>\n" +
                "      <div class=\"nav-item\" onclick=\"showTab('add-cred-tab', this)\">\n" +
                "        <span>Add Credential</span>\n" +
                "      </div>\n" +
                "    </nav>\n" +
                "  </aside>\n" +
                "  \n" +
                "  <!-- Main Body -->\n" +
                "  <main>\n" +
                "    <header>\n" +
                "      <div>\n" +
                "        <h1>SecurityDB Admin Console</h1>\n" +
                "        <p style=\"color: var(--text-muted); margin-top: 0.25rem;\">Management panel for Jettra native object storage database</p>\n" +
                "      </div>\n" +
                "      <div class=\"status-badge\">\n" +
                "        <div class=\"status-dot\"></div>\n" +
                "        <span>Database Online</span>\n" +
                "      </div>\n" +
                "    </header>\n" +
                "    \n" +
                "    <!-- Dashboard Stats Grid -->\n" +
                "    <div class=\"stats-grid\">\n" +
                "      <div class=\"stat-card\">\n" +
                "        <span class=\"stat-label\">Total Users</span>\n" +
                "        <span class=\"stat-val\" id=\"total-users\">0</span>\n" +
                "      </div>\n" +
                "      <div class=\"stat-card\">\n" +
                "        <span class=\"stat-label\">Total Credentials</span>\n" +
                "        <span class=\"stat-val\" id=\"total-creds\">0</span>\n" +
                "      </div>\n" +
                "      <div class=\"stat-card\">\n" +
                "        <span class=\"stat-label\">Total Roles</span>\n" +
                "        <span class=\"stat-val\" id=\"total-roles\">0</span>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <!-- Tabs Content -->\n" +
                "    <div id=\"users-tab\" class=\"tab-content active\">\n" +
                "      <div class=\"card\">\n" +
                "        <div class=\"card-title\">Users Registry</div>\n" +
                "        <table>\n" +
                "          <thead>\n" +
                "            <tr>\n" +
                "              <th>Name</th>\n" +
                "              <th>Email</th>\n" +
                "              <th>Phone</th>\n" +
                "              <th>Status</th>\n" +
                "              <th>Roles</th>\n" +
                "              <th>Actions</th>\n" +
                "            </tr>\n" +
                "          </thead>\n" +
                "          <tbody id=\"users-list\">\n" +
                "            <!-- Filled dynamically -->\n" +
                "          </tbody>\n" +
                "        </table>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div id=\"creds-tab\" class=\"tab-content\">\n" +
                "      <div class=\"card\">\n" +
                "        <div class=\"card-title\">Credentials Registry</div>\n" +
                "        <table>\n" +
                "          <thead>\n" +
                "            <tr>\n" +
                "              <th>User</th>\n" +
                "              <th>Username</th>\n" +
                "              <th>Password Hash</th>\n" +
                "              <th>Status</th>\n" +
                "              <th>Last Login</th>\n" +
                "              <th>Actions</th>\n" +
                "            </tr>\n" +
                "          </thead>\n" +
                "          <tbody id=\"creds-list\">\n" +
                "            <!-- Filled dynamically -->\n" +
                "          </tbody>\n" +
                "        </table>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div id=\"roles-tab\" class=\"tab-content\">\n" +
                "      <div class=\"card\">\n" +
                "        <div class=\"card-title\">Roles Registry</div>\n" +
                "        <table>\n" +
                "          <thead>\n" +
                "            <tr>\n" +
                "              <th>Role ID</th>\n" +
                "              <th>Name</th>\n" +
                "              <th>Status</th>\n" +
                "            </tr>\n" +
                "          </thead>\n" +
                "          <tbody id=\"roles-list\">\n" +
                "            <!-- Filled dynamically -->\n" +
                "          </tbody>\n" +
                "        </table>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div id=\"add-user-tab\" class=\"tab-content\">\n" +
                "      <div class=\"card\">\n" +
                "        <div class=\"card-title\">Create New User</div>\n" +
                "        <form id=\"add-user-form\" onsubmit=\"submitUserForm(event)\">\n" +
                "          <div class=\"form-grid\">\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"firstName\">First Name</label>\n" +
                "              <input type=\"text\" id=\"firstName\" required minlength=\"3\" placeholder=\"Enter first name\" />\n" +
                "            </div>\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"lastName\">Last Name</label>\n" +
                "              <input type=\"text\" id=\"lastName\" required minlength=\"3\" placeholder=\"Enter last name\" />\n" +
                "            </div>\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"email\">Email</label>\n" +
                "              <input type=\"email\" id=\"email\" placeholder=\"Enter email address\" />\n" +
                "            </div>\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"phone\">Phone</label>\n" +
                "              <input type=\"text\" id=\"phone\" placeholder=\"Enter phone number\" />\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div style=\"margin-bottom: 2rem;\">\n" +
                "            <label style=\"margin-bottom: 0.5rem; display: block;\">Assign Roles</label>\n" +
                "            <div id=\"roles-checkboxes-container\" class=\"roles-checkboxes\">\n" +
                "              <!-- Loaded dynamically -->\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class=\"checkbox-group\" style=\"margin-bottom: 2rem;\">\n" +
                "            <input type=\"checkbox\" id=\"userActive\" checked />\n" +
                "            <label for=\"userActive\">Set User as Active</label>\n" +
                "          </div>\n" +
                "          <button type=\"submit\" class=\"btn\">Create User</button>\n" +
                "        </form>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "    \n" +
                "    <div id=\"add-cred-tab\" class=\"tab-content\">\n" +
                "      <div class=\"card\">\n" +
                "        <div class=\"card-title\">Create User Credentials</div>\n" +
                "        <form id=\"add-cred-form\" onsubmit=\"submitCredForm(event)\">\n" +
                "          <div class=\"form-grid\">\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"credUser\">Select User</label>\n" +
                "              <select id=\"credUser\" required>\n" +
                "                <!-- Loaded dynamically -->\n" +
                "              </select>\n" +
                "            </div>\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"username\">Username</label>\n" +
                "              <input type=\"text\" id=\"username\" required minlength=\"7\" placeholder=\"Enter username (min 7 chars)\" />\n" +
                "            </div>\n" +
                "            <div class=\"input-group\">\n" +
                "              <label for=\"password\">Password</label>\n" +
                "              <input type=\"password\" id=\"password\" required minlength=\"6\" placeholder=\"Enter password\" />\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class=\"checkbox-group\" style=\"margin-bottom: 2rem;\">\n" +
                "            <input type=\"checkbox\" id=\"credActive\" checked />\n" +
                "            <label for=\"credActive\">Set Credentials as Active</label>\n" +
                "          </div>\n" +
                "          <button type=\"submit\" class=\"btn\">Create Credentials</button>\n" +
                "        </form>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "  \n" +
                "  <div id=\"toast-container\"></div>\n" +
                "  \n" +
                "  <script>\n" +
                "    let dbData = { roles: [], users: [], credentials: [] };\n" +
                "    \n" +
                "    async function loadData() {\n" +
                "      try {\n" +
                "        const res = await fetch('/securitydb/admin/data');\n" +
                "        dbData = await res.json();\n" +
                "        updateUI();\n" +
                "      } catch (err) {\n" +
                "        showToast('Error loading database info', 'error');\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    function updateUI() {\n" +
                "      // Stats\n" +
                "      document.getElementById('total-users').innerText = dbData.users.length;\n" +
                "      document.getElementById('total-creds').innerText = dbData.credentials.length;\n" +
                "      document.getElementById('total-roles').innerText = dbData.roles.length;\n" +
                "      \n" +
                "      // Users List\n" +
                "      const usersList = document.getElementById('users-list');\n" +
                "      usersList.innerHTML = dbData.users.length === 0 ? \n" +
                "        '<tr><td colspan=\"6\" style=\"text-align:center; color:var(--text-muted);\">No users registered</td></tr>' :\n" +
                "        dbData.users.map(u => {\n" +
                "          const rolesStr = u.jRoles.map(r => `<span class=\"badge badge-primary\">${r.name}</span>`).join(' ');\n" +
                "          const statusStr = u.active ? \n" +
                "            '<span class=\"badge badge-success\">Active</span>' :\n" +
                "            '<span class=\"badge badge-danger\">Inactive</span>';\n" +
                "          return `\n" +
                "            <tr>\n" +
                "              <td><strong>${u.firstName} ${u.lastName}</strong></td>\n" +
                "              <td>${u.email || '-'}</td>\n" +
                "              <td>${u.phone || '-'}</td>\n" +
                "              <td>${statusStr}</td>\n" +
                "              <td>${rolesStr}</td>\n" +
                "              <td>\n" +
                "                <button class=\"btn btn-danger btn-secondary\" style=\"padding:0.4rem 0.8rem; font-size:0.8rem;\" onclick=\"deleteUser('${u.id}')\">Delete</button>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          `;\n" +
                "        }).join('');\n" +
                "        \n" +
                "      // Credentials List\n" +
                "      const credsList = document.getElementById('creds-list');\n" +
                "      credsList.innerHTML = dbData.credentials.length === 0 ?\n" +
                "        '<tr><td colspan=\"6\" style=\"text-align:center; color:var(--text-muted);\">No credentials created</td></tr>' :\n" +
                "        dbData.credentials.map(c => {\n" +
                "          const userDisplayName = c.juser ? `${c.juser.firstName} ${c.juser.lastName}` : 'Unknown';\n" +
                "          const statusStr = c.active ? \n" +
                "            '<span class=\"badge badge-success\">Active</span>' :\n" +
                "            '<span class=\"badge badge-danger\">Inactive</span>';\n" +
                "          const hashPreview = c.passwordHash ? c.passwordHash.substring(0, 15) + '...' : '-';\n" +
                "          const lastLoginStr = c.lastLogin ? new Date(c.lastLogin).toLocaleString() : 'Never';\n" +
                "          return `\n" +
                "            <tr>\n" +
                "              <td>${userDisplayName}</td>\n" +
                "              <td><code>${c.username}</code></td>\n" +
                "              <td><code title=\"${c.passwordHash}\">${hashPreview}</code></td>\n" +
                "              <td>${statusStr}</td>\n" +
                "              <td>${lastLoginStr}</td>\n" +
                "              <td>\n" +
                "                <button class=\"btn btn-danger btn-secondary\" style=\"padding:0.4rem 0.8rem; font-size:0.8rem;\" onclick=\"deleteCredential('${c.id}')\">Delete</button>\n" +
                "              </td>\n" +
                "            </tr>\n" +
                "          `;\n" +
                "        }).join('');\n" +
                "        \n" +
                "      // Roles List\n" +
                "      const rolesList = document.getElementById('roles-list');\n" +
                "      rolesList.innerHTML = dbData.roles.map(r => {\n" +
                "        const statusStr = r.active ? \n" +
                "          '<span class=\"badge badge-success\">Active</span>' :\n" +
                "          '<span class=\"badge badge-danger\">Inactive</span>';\n" +
                "        return `\n" +
                "          <tr>\n" +
                "            <td><code>${r.id}</code></td>\n" +
                "            <td><strong>${r.name}</strong></td>\n" +
                "            <td>${statusStr}</td>\n" +
                "          </tr>\n" +
                "        `;\n" +
                "      }).join('');\n" +
                "      \n" +
                "      // User selector in Credential Form\n" +
                "      const credUserSelect = document.getElementById('credUser');\n" +
                "      credUserSelect.innerHTML = '<option value=\"\" disabled selected>Select a user...</option>' +\n" +
                "        dbData.users.map(u => `<option value=\"${u.id}\">${u.firstName} ${u.lastName} (${u.email || 'No email'})</option>`).join('');\n" +
                "        \n" +
                "      // Roles checkboxes in User Form\n" +
                "      const checkboxesContainer = document.getElementById('roles-checkboxes-container');\n" +
                "      checkboxesContainer.innerHTML = dbData.roles.map(r => `\n" +
                "        <div class=\"checkbox-group\">\n" +
                "          <input type=\"checkbox\" id=\"role-${r.id}\" value=\"${r.id}\" name=\"roles\" />\n" +
                "          <label for=\"role-${r.id}\">${r.name}</label>\n" +
                "        </div>\n" +
                "      `).join('');\n" +
                "    }\n" +
                "    \n" +
                "    function showTab(tabId, el) {\n" +
                "      document.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));\n" +
                "      document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));\n" +
                "      \n" +
                "      document.getElementById(tabId).classList.add('active');\n" +
                "      el.classList.add('active');\n" +
                "    }\n" +
                "    \n" +
                "    async function submitUserForm(e) {\n" +
                "      e.preventDefault();\n" +
                "      \n" +
                "      const firstName = document.getElementById('firstName').value;\n" +
                "      const lastName = document.getElementById('lastName').value;\n" +
                "      const email = document.getElementById('email').value;\n" +
                "      const phone = document.getElementById('phone').value;\n" +
                "      const active = document.getElementById('userActive').checked;\n" +
                "      \n" +
                "      const checkedRoleIds = [];\n" +
                "      dbData.roles.forEach(r => {\n" +
                "        if (document.getElementById(`role-${r.id}`).checked) {\n" +
                "          checkedRoleIds.push(r.id);\n" +
                "        }\n" +
                "      });\n" +
                "      \n" +
                "      if (checkedRoleIds.length === 0) {\n" +
                "        showToast('Please assign at least one role to the user', 'error');\n" +
                "        return;\n" +
                "      }\n" +
                "      \n" +
                "      const params = new URLSearchParams();\n" +
                "      params.append('firstName', firstName);\n" +
                "      params.append('lastName', lastName);\n" +
                "      params.append('email', email);\n" +
                "      params.append('phone', phone);\n" +
                "      params.append('active', active);\n" +
                "      checkedRoleIds.forEach(id => params.append('roles', id));\n" +
                "      \n" +
                "      try {\n" +
                "        const res = await fetch('/securitydb/admin/add-user', {\n" +
                "          method: 'POST',\n" +
                "          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "          body: params\n" +
                "        });\n" +
                "        const result = await res.json();\n" +
                "        if (result.success) {\n" +
                "          showToast('User created successfully', 'success');\n" +
                "          document.getElementById('add-user-form').reset();\n" +
                "          loadData();\n" +
                "          showTab('users-tab', document.querySelector('[onclick*=\"users-tab\"]'));\n" +
                "        } else {\n" +
                "          showToast(result.message || 'Error creating user', 'error');\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        showToast('Error sending user request', 'error');\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    async function submitCredForm(e) {\n" +
                "      e.preventDefault();\n" +
                "      \n" +
                "      const userId = document.getElementById('credUser').value;\n" +
                "      const username = document.getElementById('username').value;\n" +
                "      const password = document.getElementById('password').value;\n" +
                "      const active = document.getElementById('credActive').checked;\n" +
                "      \n" +
                "      if (!userId) {\n" +
                "        showToast('Please select a user', 'error');\n" +
                "        return;\n" +
                "      }\n" +
                "      \n" +
                "      const params = new URLSearchParams();\n" +
                "      params.append('userId', userId);\n" +
                "      params.append('username', username);\n" +
                "      params.append('password', password);\n" +
                "      params.append('active', active);\n" +
                "      \n" +
                "      try {\n" +
                "        const res = await fetch('/securitydb/admin/add-credential', {\n" +
                "          method: 'POST',\n" +
                "          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "          body: params\n" +
                "        });\n" +
                "        const result = await res.json();\n" +
                "        if (result.success) {\n" +
                "          showToast('Credentials created successfully', 'success');\n" +
                "          document.getElementById('add-cred-form').reset();\n" +
                "          loadData();\n" +
                "          showTab('creds-tab', document.querySelector('[onclick*=\"creds-tab\"]'));\n" +
                "        } else {\n" +
                "          showToast(result.message || 'Error creating credentials', 'error');\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        showToast('Error sending credential request', 'error');\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    async function deleteUser(id) {\n" +
                "      if (!confirm('Are you sure you want to delete this user? This will not delete linked credentials automatically.')) return;\n" +
                "      \n" +
                "      const params = new URLSearchParams();\n" +
                "      params.append('id', id);\n" +
                "      \n" +
                "      try {\n" +
                "        const res = await fetch('/securitydb/admin/delete-user', {\n" +
                "          method: 'POST',\n" +
                "          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "          body: params\n" +
                "        });\n" +
                "        const result = await res.json();\n" +
                "        if (result.success) {\n" +
                "          showToast('User deleted', 'success');\n" +
                "          loadData();\n" +
                "        } else {\n" +
                "          showToast(result.message || 'Error deleting user', 'error');\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        showToast('Error deleting user', 'error');\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    async function deleteCredential(id) {\n" +
                "      if (!confirm('Are you sure you want to delete these credentials?')) return;\n" +
                "      \n" +
                "      const params = new URLSearchParams();\n" +
                "      params.append('id', id);\n" +
                "      \n" +
                "      try {\n" +
                "        const res = await fetch('/securitydb/admin/delete-credential', {\n" +
                "          method: 'POST',\n" +
                "          headers: { 'Content-Type': 'application/x-www-form-urlencoded' },\n" +
                "          body: params\n" +
                "        });\n" +
                "        const result = await res.json();\n" +
                "        if (result.success) {\n" +
                "          showToast('Credentials deleted', 'success');\n" +
                "          loadData();\n" +
                "        } else {\n" +
                "          showToast(result.message || 'Error deleting credentials', 'error');\n" +
                "        }\n" +
                "      } catch (err) {\n" +
                "        showToast('Error deleting credentials', 'error');\n" +
                "      }\n" +
                "    }\n" +
                "    \n" +
                "    function showToast(message, type = 'success') {\n" +
                "      const container = document.getElementById('toast-container');\n" +
                "      const el = document.createElement('div');\n" +
                "      el.className = `toast ${type}`;\n" +
                "      el.innerHTML = `<span>${message}</span>`;\n" +
                "      container.appendChild(el);\n" +
                "      \n" +
                "      setTimeout(() => {\n" +
                "        el.style.transform = 'translateY(-20px)';\n" +
                "        el.style.opacity = '0';\n" +
                "        setTimeout(() => el.remove(), 300);\n" +
                "      }, 3000);\n" +
                "    }\n" +
                "    \n" +
                "    // Initialize\n" +
                "    window.onload = loadData;\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";

        exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private void serveData(HttpExchange exchange) throws IOException {
        Map<String, Object> data = new HashMap<>();
        data.put("roles", roleRepo.findAll());
        data.put("users", userRepo.findAll());
        data.put("credentials", credRepo.findAll());

        String json = gson.toJson(data);
        sendJsonResponse(exchange, 200, json);
    }

    private void handleAddUser(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, List<String>> params = parseFormData(body);

        String firstName = getSingleParam(params, "firstName");
        String lastName = getSingleParam(params, "lastName");
        String email = getSingleParam(params, "email");
        String phone = getSingleParam(params, "phone");
        boolean active = Boolean.parseBoolean(getSingleParam(params, "active"));
        List<String> roleIds = params.get("roles");

        Map<String, Object> result = new HashMap<>();

        if (firstName == null || firstName.length() < 3 || lastName == null || lastName.length() < 3) {
            result.put("success", false);
            result.put("message", "First name and Last name must be at least 3 characters long.");
            sendJsonResponse(exchange, 400, gson.toJson(result));
            return;
        }

        if (roleIds == null || roleIds.isEmpty()) {
            result.put("success", false);
            result.put("message", "User must be assigned at least one role.");
            sendJsonResponse(exchange, 400, gson.toJson(result));
            return;
        }

        Set<JRole> assignedRoles = new HashSet<>();
        for (String rid : roleIds) {
            Optional<JRole> rOpt = roleRepo.findById(UUID.fromString(rid));
            rOpt.ifPresent(assignedRoles::add);
        }

        try {
            JUser newUser = new JUser(
                UUID.randomUUID(),
                firstName,
                lastName,
                email,
                phone,
                active,
                assignedRoles
            );
            userRepo.save(newUser);
            result.put("success", true);
            sendJsonResponse(exchange, 200, gson.toJson(result));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            sendJsonResponse(exchange, 500, gson.toJson(result));
        }
    }

    private void handleAddCredential(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, List<String>> params = parseFormData(body);

        String userIdStr = getSingleParam(params, "userId");
        String username = getSingleParam(params, "username");
        String password = getSingleParam(params, "password");
        boolean active = Boolean.parseBoolean(getSingleParam(params, "active"));

        Map<String, Object> result = new HashMap<>();

        if (userIdStr == null || username == null || username.length() < 7 || password == null || password.length() < 6) {
            result.put("success", false);
            result.put("message", "Username must be at least 7 characters and Password must be at least 6 characters.");
            sendJsonResponse(exchange, 400, gson.toJson(result));
            return;
        }

        Optional<JUser> userOpt = userRepo.findById(UUID.fromString(userIdStr));
        if (userOpt.isEmpty()) {
            result.put("success", false);
            result.put("message", "Selected user not found.");
            sendJsonResponse(exchange, 400, gson.toJson(result));
            return;
        }

        try {
            String passwordHash = JettraSecurityDBInitializer.hashPassword(password);
            JCredential credential = new JCredential(
                UUID.randomUUID(),
                userOpt.get(),
                username,
                passwordHash,
                active,
                null // lastLogin
            );
            credRepo.save(credential);
            result.put("success", true);
            sendJsonResponse(exchange, 200, gson.toJson(result));
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
            sendJsonResponse(exchange, 500, gson.toJson(result));
        }
    }

    private void handleDeleteUser(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, List<String>> params = parseFormData(body);
        String idStr = getSingleParam(params, "id");

        Map<String, Object> result = new HashMap<>();
        if (idStr != null) {
            userRepo.delete(UUID.fromString(idStr));
            result.put("success", true);
            sendJsonResponse(exchange, 200, gson.toJson(result));
        } else {
            result.put("success", false);
            result.put("message", "Missing ID parameter");
            sendJsonResponse(exchange, 400, gson.toJson(result));
        }
    }

    private void handleDeleteCredential(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        Map<String, List<String>> params = parseFormData(body);
        String idStr = getSingleParam(params, "id");

        Map<String, Object> result = new HashMap<>();
        if (idStr != null) {
            credRepo.delete(UUID.fromString(idStr));
            result.put("success", true);
            sendJsonResponse(exchange, 200, gson.toJson(result));
        } else {
            result.put("success", false);
            result.put("message", "Missing ID parameter");
            sendJsonResponse(exchange, 400, gson.toJson(result));
        }
    }

    private String getSingleParam(Map<String, List<String>> params, String key) {
        List<String> vals = params.get(key);
        return (vals != null && !vals.isEmpty()) ? vals.get(0) : null;
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private Map<String, List<String>> parseFormData(String query) {
        Map<String, List<String>> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            try {
                String key = URLDecoder.decode(idx > 0 ? pair.substring(0, idx) : pair, "UTF-8");
                String value = URLDecoder.decode(idx > 0 && pair.length() > idx + 1 ? pair.substring(idx + 1) : "", "UTF-8");
                map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
            } catch (Exception e) {
                // Ignore
            }
        }
        return map;
    }

    private void sendJsonResponse(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        sendJsonResponse(exchange, status, gson.toJson(error));
    }
}
