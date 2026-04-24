import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.beans.property.*;

import java.net.URL;
import java.sql.*;
import java.util.*;

// ===========================================================
// PestGuard PH — Actions Controller
// Handles: Auth (login/register), Pest & Disease CRUD
// ===========================================================

public class Actions implements Initializable {

    // ─────────────────────────────────────────────────────────
    // SESSION — stores the currently logged-in user
    // ─────────────────────────────────────────────────────────
    public static int    SESSION_USER_ID   = -1;
    public static String SESSION_USERNAME  = "";
    public static String SESSION_FULLNAME  = "";

    // ─────────────────────────────────────────────────────────
    // LOGIN FXML FIELDS
    // ─────────────────────────────────────────────────────────
    @FXML private TextField     loginUsername;
    @FXML private PasswordField loginPassword;
    @FXML private Label         loginError;

    // ─────────────────────────────────────────────────────────
    // REGISTER FXML FIELDS
    // ─────────────────────────────────────────────────────────
    @FXML private TextField     regFullName;
    @FXML private TextField     regUsername;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label         regMessage;

    // ─────────────────────────────────────────────────────────
    // DASHBOARD FXML FIELDS
    // ─────────────────────────────────────────────────────────

    // Sidebar / Topbar
    @FXML private Label sidebarUsername;
    @FXML private Label sidebarFullName;
    @FXML private Label topbarTitle;
    @FXML private Label topbarUser;
    @FXML private Label topbarChip;

    // Nav buttons (for active state toggling)
    @FXML private Button navDashboard;
    @FXML private Button navRecords;

    // View containers
    @FXML private VBox dashboardView;
    @FXML private VBox recordsView;
    @FXML private VBox formView;

    // Stat labels
    @FXML private Label statTotal;
    @FXML private Label statPests;
    @FXML private Label statDiseases;
    @FXML private Label statCritical;

    // Dashboard (recent) table
    @FXML private TableView<PestRecord>    dashboardTable;
    @FXML private TableColumn<PestRecord, Integer> colDashId;
    @FXML private TableColumn<PestRecord, String>  colDashName;
    @FXML private TableColumn<PestRecord, String>  colDashType;
    @FXML private TableColumn<PestRecord, String>  colDashSeverity;
    @FXML private TableColumn<PestRecord, String>  colDashLocation;
    @FXML private TableColumn<PestRecord, String>  colDashDate;

    // Records full table
    @FXML private TableView<PestRecord>    recordsTable;
    @FXML private TableColumn<PestRecord, Integer> colId;
    @FXML private TableColumn<PestRecord, String>  colName;
    @FXML private TableColumn<PestRecord, String>  colType;
    @FXML private TableColumn<PestRecord, String>  colSeverity;
    @FXML private TableColumn<PestRecord, String>  colLocation;
    @FXML private TableColumn<PestRecord, String>  colAffected;
    @FXML private TableColumn<PestRecord, String>  colDate;
    @FXML private TableColumn<PestRecord, Void>    colActions;

    // Search
    @FXML private TextField searchField;

    // Form fields
    @FXML private TextField  formId;
    @FXML private TextField  formName;
    @FXML private ComboBox<String> formType;
    @FXML private ComboBox<String> formSeverity;
    @FXML private TextField  formLocation;
    @FXML private TextField  formAffectedArea;
    @FXML private TextField  formDate;
    @FXML private TextArea   formSymptoms;
    @FXML private TextArea   formTreatment;
    @FXML private Label      formError;
    @FXML private Label      formTitle;
    @FXML private Label      formSub;
    @FXML private Button     formSubmitBtn;

    // ─────────────────────────────────────────────────────────
    // DATA
    // ─────────────────────────────────────────────────────────
    private ObservableList<PestRecord> allRecords = FXCollections.observableArrayList();
    private FilteredList<PestRecord>   filteredRecords;

    // ─────────────────────────────────────────────────────────
    // INITIALIZE — runs after FXML is loaded
    // ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (dashboardView != null) {
            setupDashboard();
        }
    }

    // ─────────────────────────────────────────────────────────
    // DATABASE SETUP
    // ─────────────────────────────────────────────────────────

    private void initDatabase() {
        // Create users table
        transactionalSQLiteQuery(
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id        INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  full_name TEXT    NOT NULL," +
            "  username  TEXT    NOT NULL UNIQUE," +
            "  password  TEXT    NOT NULL," +
            "  created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
            ")",
            null
        );

        // Create pest_records table
        transactionalSQLiteQuery(
            "CREATE TABLE IF NOT EXISTS pest_records (" +
            "  id            INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id       INTEGER NOT NULL," +
            "  name          TEXT    NOT NULL," +
            "  type          TEXT    NOT NULL," +
            "  severity      TEXT    NOT NULL," +
            "  location      TEXT    NOT NULL," +
            "  affected_area TEXT," +
            "  date_observed TEXT    NOT NULL," +
            "  symptoms      TEXT," +
            "  treatment     TEXT," +
            "  created_at    DATETIME DEFAULT CURRENT_TIMESTAMP," +
            "  FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")",
            null
        );
    }

    // ─────────────────────────────────────────────────────────
    // ROUTING
    // ─────────────────────────────────────────────────────────

    public void goToPage(ActionEvent event) {
        Button b = (Button) event.getSource();
        Main.go(b.getUserData().toString());
    }

    // ─────────────────────────────────────────────────────────
    // AUTH — LOGIN
    // ─────────────────────────────────────────────────────────

    @FXML
    public void handleLogin(ActionEvent event) {
        String username = loginUsername.getText().trim();
        String password = loginPassword.getText();

        initDatabase();

        // Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError(loginError, "Please fill in all fields.");
            return;
        }

        // Query user
        Object result = transactionalSQLiteQuery(
            "SELECT id, full_name, username, password FROM users WHERE username = ?",
            Arrays.asList(username)
        );

        if (result instanceof String) {
            showError(loginError, "Database error: " + result);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) result;

        if (rows.isEmpty()) {
            showError(loginError, "Invalid username or password.");
            return;
        }

        Map<String, Object> user = rows.get(0);
        String storedHash = (String) user.get("password");

        // Simple SHA-256 hash check
        if (!hashPassword(password).equals(storedHash)) {
            showError(loginError, "Invalid username or password.");
            return;
        }

        // Set session
        SESSION_USER_ID  = ((Number) user.get("id")).intValue();
        SESSION_USERNAME = (String) user.get("username");
        SESSION_FULLNAME = (String) user.get("full_name");

        // Navigate to dashboard
        Main.go("Dashboard");
    }

    // ─────────────────────────────────────────────────────────
    // AUTH — REGISTER
    // ─────────────────────────────────────────────────────────

    @FXML
    public void handleRegister(ActionEvent event) {
        String fullName = regFullName.getText().trim();
        String username = regUsername.getText().trim();
        String password = regPassword.getText();
        String confirm  = regConfirmPassword.getText();

        initDatabase();

        // Validation
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showError(regMessage, "Please fill in all fields.");
            return;
        }
        if (password.length() < 6) {
            showError(regMessage, "Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError(regMessage, "Passwords do not match.");
            return;
        }

        // Check username taken
        Object checkResult = transactionalSQLiteQuery(
            "SELECT id FROM users WHERE username = ?",
            Arrays.asList(username)
        );

        if (checkResult instanceof String) {
            showError(regMessage, "Database error: " + checkResult);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> existing = (List<Map<String, Object>>) checkResult;

        if (!existing.isEmpty()) {
            showError(regMessage, "Username is already taken. Choose another.");
            return;
        }

        // Insert user
        Object insertResult = transactionalSQLiteQuery(
            "INSERT INTO users (full_name, username, password) VALUES (?, ?, ?)",
            Arrays.asList(fullName, username, hashPassword(password))
        );

        if (insertResult instanceof Boolean && (Boolean) insertResult) {
            // Show success then redirect to login
            showSuccess(regMessage, "Account created! Redirecting to login...");
            javafx.application.Platform.runLater(() -> {
                try { Thread.sleep(1200); } catch (Exception ignored) {}
                javafx.application.Platform.runLater(() -> Main.go("Login"));
            });
        } else {
            showError(regMessage, "Registration failed: " + insertResult);
        }
    }

    // ─────────────────────────────────────────────────────────
    // AUTH — LOGOUT
    // ─────────────────────────────────────────────────────────

    @FXML
    public void handleLogout(ActionEvent event) {
        SESSION_USER_ID  = -1;
        SESSION_USERNAME = "";
        SESSION_FULLNAME = "";
        Main.go("Login");
    }

    // ─────────────────────────────────────────────────────────
    // DASHBOARD SETUP
    // ─────────────────────────────────────────────────────────

    private void setupDashboard() {
        // Session info in sidebar/topbar
        if (sidebarUsername != null) sidebarUsername.setText("@" + SESSION_USERNAME);
        if (sidebarFullName  != null) sidebarFullName.setText(SESSION_FULLNAME);
        if (topbarUser       != null) topbarUser.setText("Logged in as");
        if (topbarChip       != null) topbarChip.setText("@" + SESSION_USERNAME);

        // Setup table columns
        setupTableColumns();

        // Load data
        loadAllRecords();

        // Show dashboard view by default
        switchView("dashboard");
    }

    // ─────────────────────────────────────────────────────────
    // TABLE COLUMN SETUP
    // ─────────────────────────────────────────────────────────

    private void setupTableColumns() {
        // Dashboard table
        colDashId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDashName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDashType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colDashSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colDashLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colDashDate.setCellValueFactory(new PropertyValueFactory<>("dateObserved"));

        // Full records table
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severity"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colAffected.setCellValueFactory(new PropertyValueFactory<>("affectedArea"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateObserved"));

        // Actions column with Edit + Delete buttons
        colActions.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-edit");
                deleteBtn.getStyleClass().add("btn-danger");

                editBtn.setOnAction(e -> {
                    PestRecord rec = getTableView().getItems().get(getIndex());
                    openEditForm(rec);
                });
                deleteBtn.setOnAction(e -> {
                    PestRecord rec = getTableView().getItems().get(getIndex());
                    confirmDelete(rec);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    // ─────────────────────────────────────────────────────────
    // LOAD ALL RECORDS
    // ─────────────────────────────────────────────────────────

    private void loadAllRecords() {
        allRecords.clear();

        Object result = transactionalSQLiteQuery(
            "SELECT * FROM pest_records WHERE user_id = ? ORDER BY id DESC",
            Arrays.asList(SESSION_USER_ID)
        );

        if (result instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) result;
            for (Map<String, Object> row : rows) {
                allRecords.add(mapToRecord(row));
            }
        }

        // Update stat cards
        updateStats();

        // Dashboard table shows only recent 10
        ObservableList<PestRecord> recent = FXCollections.observableArrayList(
            allRecords.subList(0, Math.min(10, allRecords.size()))
        );
        dashboardTable.setItems(recent);

        // Full records table with search filter
        filteredRecords = new FilteredList<>(allRecords, p -> true);
        recordsTable.setItems(filteredRecords);
    }

    private PestRecord mapToRecord(Map<String, Object> row) {
        PestRecord r = new PestRecord();
        r.setId(     toInt(row.get("id")));
        r.setName(   str(row.get("name")));
        r.setType(   str(row.get("type")));
        r.setSeverity(str(row.get("severity")));
        r.setLocation(str(row.get("location")));
        r.setAffectedArea(str(row.get("affected_area")));
        r.setDateObserved(str(row.get("date_observed")));
        r.setSymptoms( str(row.get("symptoms")));
        r.setTreatment(str(row.get("treatment")));
        return r;
    }

    private void updateStats() {
        statTotal.setText(String.valueOf(allRecords.size()));

        long pests    = allRecords.stream().filter(r -> r.getType().toLowerCase().contains("pest") || r.getType().toLowerCase().contains("insect") || r.getType().toLowerCase().contains("mite") || r.getType().toLowerCase().contains("nematode")).count();
        long diseases = allRecords.stream().filter(r -> r.getType().toLowerCase().contains("disease") || r.getType().toLowerCase().contains("fungal") || r.getType().toLowerCase().contains("bacterial") || r.getType().toLowerCase().contains("viral")).count();
        long critical = allRecords.stream().filter(r -> r.getSeverity().equalsIgnoreCase("Critical")).count();

        statPests.setText(String.valueOf(pests));
        statDiseases.setText(String.valueOf(diseases));
        statCritical.setText(String.valueOf(critical));
    }

    // ─────────────────────────────────────────────────────────
    // VIEW SWITCHING
    // ─────────────────────────────────────────────────────────

    private void switchView(String view) {
        setVisible(dashboardView, view.equals("dashboard"));
        setVisible(recordsView,   view.equals("records"));
        setVisible(formView,      view.equals("form"));

        // Update topbar title
        if (topbarTitle != null) {
            switch (view) {
                case "dashboard" -> topbarTitle.setText("Dashboard");
                case "records"   -> topbarTitle.setText("Records");
                case "form"      -> topbarTitle.setText("Record Form");
            }
        }

        // Update nav active state
        updateNavActive(view);
    }

    private void updateNavActive(String view) {
        if (navDashboard == null || navRecords == null) return;
        navDashboard.getStyleClass().removeAll("nav-btn-active");
        navRecords.getStyleClass().removeAll("nav-btn-active");

        if (view.equals("dashboard")) navDashboard.getStyleClass().add("nav-btn-active");
        if (view.equals("records") || view.equals("form")) navRecords.getStyleClass().add("nav-btn-active");
    }

    @FXML
    public void showDashboardView(ActionEvent event) {
        loadAllRecords();
        switchView("dashboard");
    }

    @FXML
    public void showRecordsView(ActionEvent event) {
        loadAllRecords();
        switchView("records");
    }

    // Overload for internal calls (no event)
    public void showRecordsView() {
        loadAllRecords();
        switchView("records");
    }

    // ─────────────────────────────────────────────────────────
    // SEARCH
    // ─────────────────────────────────────────────────────────

    @FXML
    public void handleSearch(javafx.scene.input.KeyEvent event) {
        String query = searchField.getText().toLowerCase().trim();
        if (filteredRecords == null) return;
        filteredRecords.setPredicate(rec -> {
            if (query.isEmpty()) return true;
            return rec.getName().toLowerCase().contains(query)
                || rec.getLocation().toLowerCase().contains(query)
                || rec.getType().toLowerCase().contains(query)
                || rec.getSeverity().toLowerCase().contains(query);
        });
    }

    // ─────────────────────────────────────────────────────────
    // ADD / EDIT FORM
    // ─────────────────────────────────────────────────────────

    @FXML
    public void showAddForm(ActionEvent event) {
        clearForm();
        formId.setText("");
        formTitle.setText("Add New Record");
        formSub.setText("Document a new pest or disease case");
        formSubmitBtn.setText("Save Record");
        hideError(formError);
        switchView("form");
    }

    private void openEditForm(PestRecord rec) {
        clearForm();
        formId.setText(String.valueOf(rec.getId()));
        formName.setText(rec.getName());
        formType.setValue(rec.getType());
        formSeverity.setValue(rec.getSeverity());
        formLocation.setText(rec.getLocation());
        formAffectedArea.setText(rec.getAffectedArea());
        formDate.setText(rec.getDateObserved());
        formSymptoms.setText(rec.getSymptoms());
        formTreatment.setText(rec.getTreatment());
        formTitle.setText("Edit Record");
        formSub.setText("Update the details for: " + rec.getName());
        formSubmitBtn.setText("Update Record");
        hideError(formError);
        switchView("form");
    }

    private void clearForm() {
        formName.clear();
        formType.setValue(null);
        formSeverity.setValue(null);
        formLocation.clear();
        formAffectedArea.clear();
        formDate.clear();
        formSymptoms.clear();
        formTreatment.clear();
    }

    // ─────────────────────────────────────────────────────────
    // SAVE RECORD (INSERT or UPDATE)
    // ─────────────────────────────────────────────────────────

    @FXML
    public void handleSaveRecord(ActionEvent event) {
        String name         = formName.getText().trim();
        String type         = formType.getValue();
        String severity     = formSeverity.getValue();
        String location     = formLocation.getText().trim();
        String affectedArea = formAffectedArea.getText().trim();
        String date         = formDate.getText().trim();
        String symptoms     = formSymptoms.getText().trim();
        String treatment    = formTreatment.getText().trim();
        String idStr        = formId.getText().trim();

        // Validation
        if (name.isEmpty() || type == null || severity == null || location.isEmpty() || date.isEmpty()) {
            showError(formError, "Please fill in all required fields (marked with *).");
            return;
        }

        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            showError(formError, "Date must be in YYYY-MM-DD format (e.g. 2024-06-15).");
            return;
        }

        boolean isEdit = !idStr.isEmpty();

        Object result;
        if (isEdit) {
            // UPDATE
            result = transactionalSQLiteQuery(
                "UPDATE pest_records SET name=?, type=?, severity=?, location=?, affected_area=?, date_observed=?, symptoms=?, treatment=? WHERE id=? AND user_id=?",
                Arrays.asList(name, type, severity, location, affectedArea, date, symptoms, treatment, Integer.parseInt(idStr), SESSION_USER_ID)
            );
        } else {
            // INSERT
            result = transactionalSQLiteQuery(
                "INSERT INTO pest_records (user_id, name, type, severity, location, affected_area, date_observed, symptoms, treatment) VALUES (?,?,?,?,?,?,?,?,?)",
                Arrays.asList(SESSION_USER_ID, name, type, severity, location, affectedArea, date, symptoms, treatment)
            );
        }

        if (result instanceof Boolean && (Boolean) result) {
            showRecordsView();
        } else {
            showError(formError, "Save failed: " + result);
        }
    }

    // ─────────────────────────────────────────────────────────
    // DELETE RECORD
    // ─────────────────────────────────────────────────────────

    private void confirmDelete(PestRecord rec) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Record");
        alert.setHeaderText("Delete \"" + rec.getName() + "\"?");
        alert.setContentText("This action cannot be undone.");

        // Style the dialog
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("style.css").toExternalForm()
        );

        Optional<ButtonType> response = alert.showAndWait();
        if (response.isPresent() && response.get() == ButtonType.OK) {
            transactionalSQLiteQuery(
                "DELETE FROM pest_records WHERE id = ? AND user_id = ?",
                Arrays.asList(rec.getId(), SESSION_USER_ID)
            );
            loadAllRecords();
            switchView("records");
        }
    }

    // ─────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────

    private void showError(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #cf6b6b; -fx-background-color: #3d1a1a; -fx-background-radius: 6; -fx-padding: 6 12;");
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void showSuccess(Label lbl, String msg) {
        lbl.setText(msg);
        lbl.setStyle("-fx-text-fill: #6bcf7f; -fx-background-color: #1a3d1c; -fx-background-radius: 6; -fx-padding: 6 12;");
        lbl.setVisible(true);
        lbl.setManaged(true);
    }

    private void hideError(Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    private void setVisible(VBox box, boolean visible) {
        box.setVisible(visible);
        box.setManaged(visible);
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }
    private int    toInt(Object o) { return o == null ? 0 : ((Number) o).intValue(); }

    /**
     * Simple SHA-256 password hashing.
     */
    private String hashPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return password; // fallback (shouldn't happen)
        }
    }

    // ─────────────────────────────────────────────────────────
    // SQLITE QUERY FUNCTION
    // ─────────────────────────────────────────────────────────
    public static Object transactionalSQLiteQuery(String query, List<Object> params) {
        String url = "jdbc:sqlite:database.db";
        boolean isSelect = query.trim().toUpperCase().startsWith("SELECT");

        try (Connection conn = DriverManager.getConnection(url)) {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(query);

            if (params != null) {
                for (int i = 0; i < params.size(); i++) {
                    stmt.setObject(i + 1, params.get(i));
                }
            }

            if (isSelect) {
                ResultSet rs = stmt.executeQuery();
                List<Map<String, Object>> result = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    result.add(row);
                }
                stmt.close();
                conn.commit();
                return result;
            }

            stmt.executeUpdate();
            stmt.close();
            conn.commit();
            return true;

        } catch (Exception e) {
            return "SQLite error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────
    // MYSQL QUERY FUNCTION (kept for reference)
    // ─────────────────────────────────────────────────────────
    public static Object transactionalMySQLQuery(String query, List<Object> params) {
        String url  = "jdbc:mysql://localhost:3306/mydb";
        String user = "root";
        String pass = "password";
        boolean isSelect = query.trim().toUpperCase().startsWith("SELECT");

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);
            PreparedStatement stmt = conn.prepareStatement(query);
            if (params != null) {
                for (int i = 0; i < params.size(); i++) stmt.setObject(i + 1, params.get(i));
            }
            if (isSelect) {
                ResultSet rs = stmt.executeQuery();
                List<Map<String, Object>> result = new ArrayList<>();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) row.put(meta.getColumnName(i), rs.getObject(i));
                    result.add(row);
                }
                stmt.close(); conn.commit();
                return result;
            }
            stmt.executeUpdate(); stmt.close(); conn.commit();
            return true;
        } catch (Exception e) {
            return "MySQL error: " + e.getMessage();
        }
    }

    // ─────────────────────────────────────────────────────────
    // PEST RECORD MODEL (inner class)
    // ─────────────────────────────────────────────────────────

    public static class PestRecord {
        private final IntegerProperty    id           = new SimpleIntegerProperty();
        private final StringProperty     name         = new SimpleStringProperty();
        private final StringProperty     type         = new SimpleStringProperty();
        private final StringProperty     severity     = new SimpleStringProperty();
        private final StringProperty     location     = new SimpleStringProperty();
        private final StringProperty     affectedArea = new SimpleStringProperty();
        private final StringProperty     dateObserved = new SimpleStringProperty();
        private final StringProperty     symptoms     = new SimpleStringProperty();
        private final StringProperty     treatment    = new SimpleStringProperty();

        public int    getId()           { return id.get(); }
        public void   setId(int v)      { id.set(v); }
        public IntegerProperty idProperty() { return id; }

        public String getName()         { return name.get(); }
        public void   setName(String v) { name.set(v); }
        public StringProperty nameProperty() { return name; }

        public String getType()         { return type.get(); }
        public void   setType(String v) { type.set(v); }
        public StringProperty typeProperty() { return type; }

        public String getSeverity()         { return severity.get(); }
        public void   setSeverity(String v) { severity.set(v); }
        public StringProperty severityProperty() { return severity; }

        public String getLocation()         { return location.get(); }
        public void   setLocation(String v) { location.set(v); }
        public StringProperty locationProperty() { return location; }

        public String getAffectedArea()         { return affectedArea.get(); }
        public void   setAffectedArea(String v) { affectedArea.set(v); }
        public StringProperty affectedAreaProperty() { return affectedArea; }

        public String getDateObserved()         { return dateObserved.get(); }
        public void   setDateObserved(String v) { dateObserved.set(v); }
        public StringProperty dateObservedProperty() { return dateObserved; }

        public String getSymptoms()         { return symptoms.get(); }
        public void   setSymptoms(String v) { symptoms.set(v); }
        public StringProperty symptomsProperty() { return symptoms; }

        public String getTreatment()         { return treatment.get(); }
        public void   setTreatment(String v) { treatment.set(v); }
        public StringProperty treatmentProperty() { return treatment; }
    }
}