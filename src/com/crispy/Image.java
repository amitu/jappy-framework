package com.crispy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import com.amazonaws.services.s3.model.Bucket;

@WebServlet(urlPatterns = { "/resource", "/resource/*" })
public class Image extends HttpServlet {

	private static AtomicLong mID = new AtomicLong(System.currentTimeMillis());

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String fileName = req.getPathInfo();
		if (fileName.startsWith("/class")) {
			doClass(fileName.substring(fileName.indexOf('/', 1)), resp);
		} else if (fileName.startsWith("/local")) {
			doLocal(fileName.substring(fileName.indexOf('/', 1)), resp);
		}
	}

	private void doClass(String path, HttpServletResponse resp)
			throws IOException {
		IOUtils.copy(getClass().getResourceAsStream(path),
				resp.getOutputStream());
		resp.getOutputStream().flush();
	}

	private void doLocal(String path, HttpServletResponse resp)
			throws IOException {
		String extension = path.substring(path.lastIndexOf('.') + 1);
		if (extension.equals("jpg")) {
			extension = "jpeg";
		}
		File realFile = new File(path);
		if (!realFile.exists()) {
			resp.setStatus(402);
			return;
		}
		resp.setContentType("image/" + extension);
		IOUtils.copy(new FileInputStream(realFile), resp.getOutputStream());
		resp.getOutputStream().flush();
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			String sourceFileName = req.getHeader("X-File-Name");
			String uploadFolder = req.getParameter("folder");
			String s3Bucket = req.getParameter("bucket");

			if (uploadFolder != null) {
				resp.getWriter().write(
						new JSONObject()
								.put("success", true)
								.put("value",
										uploadFile(uploadFolder,
												req.getInputStream(),
												sourceFileName)).toString());
				resp.getWriter().flush();
			} else if (s3Bucket != null) {
				resp.getWriter().write(
						new JSONObject()
								.put("success", true)
								.put("value",
										uploadS3(s3Bucket,
												req.getInputStream(),
												sourceFileName)).toString());
				resp.getWriter().flush();
			}
		} catch (Exception e) {
			e.printStackTrace();
			resp.setStatus(resp.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().print("{success: false}");
			resp.getWriter().flush();
		}
	}

	static String uploadFile(String uploadFolder, InputStream input,
			String fileName) throws FileNotFoundException, IOException {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		long nextID = mID.incrementAndGet();
		File folder = new File(uploadFolder);
		folder.mkdirs();
		File f = new File(folder, nextID + "." + ext);
		IOUtils.copy(input, new FileOutputStream(f));
		return f.getAbsolutePath().toString();

	}

	static String uploadS3(String s3Bucket, InputStream input, String fileName)
			throws FileNotFoundException, IOException {
		String ext = fileName.substring(fileName.lastIndexOf('.') + 1);
		long nextID = mID.incrementAndGet();
		String s3Comps[] = s3Bucket.split("/");

		String bucket = s3Comps[0];
		String parent = (s3Comps.length == 1) ? "" : (s3Comps[1] + "/");

		File tmp = File.createTempFile("image", ext);
		IOUtils.copy(input, new FileOutputStream(tmp));
		Cloud.s3(bucket).allowRead().upload(parent + nextID + "." + ext, tmp);
		return "http://" + bucket + ".s3.amazonaws.com/" + parent + nextID
				+ "." + ext;
	}
}
