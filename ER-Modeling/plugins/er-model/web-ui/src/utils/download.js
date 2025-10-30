/**
 * Triggers a browser download from a data URL.
 * @param {string} dataUrl The data URL (e.g., "data:text/plain;base64,SGVsbG8sIFdvcmxkIQ==")
 * @param {string} filename The desired name for the downloaded file.
 */
export function triggerDownload(dataUrl, filename) {
  const link = document.createElement('a');
  link.href = dataUrl;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();

  // Clean up the link element
  setTimeout(() => {
    document.body.removeChild(link);
  }, 0);
}
