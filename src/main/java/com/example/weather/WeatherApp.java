package com.example.weather;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

// Using a tiny built-in parser to avoid external JSON dependencies

public class WeatherApp {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> createAndShow());
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("Weather Forecast");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 480);

        GradientPanel content = new GradientPanel();
        content.setLayout(new BorderLayout(8,8));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        top.setOpaque(false);
        top.add(new JLabel("Location:"));
        JTextField locationField = new JTextField("San Francisco", 20);
        top.add(locationField);

        JButton fetchBtn = new JButton("Fetch Forecast");
        top.add(fetchBtn);

        content.add(top, BorderLayout.NORTH);

        JTextArea output = new JTextArea();
        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setOpaque(false);
        output.setForeground(Color.white);

        JScrollPane scroll = new JScrollPane(output);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));

        content.add(scroll, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        fetchBtn.addActionListener(e -> {
            fetchBtn.setEnabled(false);
            output.setText("Fetching...\n");
            String location = locationField.getText().trim();

            new SwingWorker<String, Void>() {
                protected String doInBackground() throws Exception {
                    System.out.println("Geocoding location: " + location);
                    // first geocode the location string
                    double[] coords = geocode(location);
                    if (coords == null) throw new IOException("Location not found");
                    System.out.println("Geocode result: lat=" + coords[0] + ", lon=" + coords[1]);
                        String url = String.format(
                            "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s&current_weather=true&daily=temperature_2m_max,temperature_2m_min&timezone=auto",
                            URLEncoder.encode(String.valueOf(coords[0]), "UTF-8"), URLEncoder.encode(String.valueOf(coords[1]), "UTF-8"));
                    System.out.println("Fetching forecast from: " + url);
                        HttpClient client = HttpClient.newHttpClient();
                        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
                        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                    System.out.println("Forecast HTTP status: " + resp.statusCode());
                    if (resp.statusCode() != 200) {
                        throw new IOException("HTTP " + resp.statusCode());
                    }
                    return resp.body();
                }

                protected void done() {
                    try {
                        String body = get();
                        StringBuilder sb = new StringBuilder();

                        String currObj = extractObject(body, "current_weather");
                        if (currObj != null) {
                            String temp = extractValue(currObj, "temperature");
                            String wind = extractValue(currObj, "windspeed");
                            sb.append("Current: ").append(temp).append(" °C, wind ").append(wind).append(" m/s\n\n");
                        }

                        String dailyObj = extractObject(body, "daily");
                        if (dailyObj != null) {
                            java.util.List<String> times = extractArray(dailyObj, "time");
                            java.util.List<String> mins = extractArray(dailyObj, "temperature_2m_min");
                            java.util.List<String> maxs = extractArray(dailyObj, "temperature_2m_max");

                            sb.append("Daily Forecast:\n");
                            int n = Math.min(times.size(), Math.min(mins.size(), maxs.size()));
                            for (int i = 0; i < n; i++) {
                                sb.append(times.get(i)).append(": min ").append(mins.get(i)).append(" °C, max ")
                                        .append(maxs.get(i)).append(" °C\n");
                            }
                        }

                        output.setText(sb.toString());
                    } catch (Exception ex) {
                        String msg = ex.getMessage();
                        output.setText("Error: " + msg);
                        System.err.println("Error during fetch: " + ex);
                        JOptionPane.showMessageDialog(frame, ex.toString(), "Error", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        fetchBtn.setEnabled(true);
                    }
                }
            }.execute();
        });
    }

    static class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            int w = getWidth();
            int h = getHeight();
            GradientPaint gp = new GradientPaint(0, 0, new Color(102, 51, 153), 0, h, new Color(204, 153, 255));
            g2.setPaint(gp);
            g2.fillRect(0, 0, w, h);
            super.paintComponent(g);
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    }

    // --- Tiny JSON helpers (very lenient, adequate for the API responses used here) ---
    private static String extractObject(String json, String key) {
        int k = json.indexOf('"' + key + '"');
        if (k == -1) return null;
        int brace = json.indexOf('{', k);
        if (brace == -1) return null;
        int end = findMatching(json, brace, '{', '}');
        if (end == -1) return null;
        return json.substring(brace, end + 1);
    }

    private static double[] geocode(String location) throws Exception {
        String url = "https://geocoding-api.open-meteo.com/v1/search?name=" + URLEncoder.encode(location, "UTF-8") + "&count=1";
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) return null;
        String body = resp.body();
        // find first object in results array
        int k = body.indexOf("\"results\"");
        if (k == -1) return null;
        int b = body.indexOf('[', k);
        if (b == -1) return null;
        int objStart = body.indexOf('{', b);
        if (objStart == -1) return null;
        int objEnd = findMatching(body, objStart, '{', '}');
        if (objEnd == -1) return null;
        String first = body.substring(objStart, objEnd + 1);
        String lat = extractValue(first, "latitude");
        String lon = extractValue(first, "longitude");
        if (lat == null || lat.isEmpty() || lon == null || lon.isEmpty()) return null;
        return new double[] { Double.parseDouble(lat), Double.parseDouble(lon) };
    }

    private static int findMatching(String s, int start, char open, char close) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == open) depth++;
            else if (c == close) {
                depth--;
                if (depth == 0) return i;
            }
            else if (c == '"') { // skip strings
                i++;
                while (i < s.length()) {
                    char d = s.charAt(i);
                    if (d == '\\') i += 2;
                    else if (d == '"') break;
                    else i++;
                }
            }
        }
        return -1;
    }

    private static String extractValue(String obj, String key) {
        int k = obj.indexOf('"' + key + '"');
        if (k == -1) return "";
        int colon = obj.indexOf(':', k);
        if (colon == -1) return "";
        int i = colon + 1;
        while (i < obj.length() && Character.isWhitespace(obj.charAt(i))) i++;
        if (i < obj.length() && obj.charAt(i) == '"') {
            i++;
            StringBuilder sb = new StringBuilder();
            while (i < obj.length()) {
                char c = obj.charAt(i);
                if (c == '\\' && i + 1 < obj.length()) { sb.append(obj.charAt(i+1)); i += 2; }
                else if (c == '"') break;
                else { sb.append(c); i++; }
            }
            return sb.toString();
        } else {
            StringBuilder sb = new StringBuilder();
            while (i < obj.length()) {
                char c = obj.charAt(i);
                if (c == ',' || c == '}' || c == ']') break;
                sb.append(c); i++;
            }
            return sb.toString().trim();
        }
    }

    private static java.util.List<String> extractArray(String obj, String key) {
        java.util.List<String> out = new java.util.ArrayList<>();
        int k = obj.indexOf('"' + key + '"');
        if (k == -1) return out;
        int b = obj.indexOf('[', k);
        if (b == -1) return out;
        int end = findMatching(obj, b, '[', ']');
        if (end == -1) return out;
        String inner = obj.substring(b + 1, end);
        boolean inQuotes = false;
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '"') { inQuotes = !inQuotes; continue; }
            if (c == ',' && !inQuotes) {
                String token = cur.toString().trim();
                if (token.startsWith("\"") && token.endsWith("\"")) token = token.substring(1, token.length()-1);
                out.add(stripQuotes(token));
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        String last = cur.toString().trim();
        if (!last.isEmpty()) out.add(stripQuotes(last));
        return out;
    }

    private static String stripQuotes(String s) {
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) return s.substring(1, s.length()-1);
        return s;
    }
}
