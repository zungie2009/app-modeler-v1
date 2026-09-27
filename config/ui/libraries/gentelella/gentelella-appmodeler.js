/*
 * Gentelella v4.2.0 App Modeler runtime bridge.
 * Keeps browser-only delegated interactions separate from App Modeler's Vue contract.
 */
document.addEventListener('click', (e) => {
  const toggle = e.target.closest('.toggle');
  if (toggle) toggle.classList.toggle('on');

  const tab = e.target.closest('.chart-tab');
  if (tab && tab.parentElement) {
    tab.parentElement.querySelectorAll('.chart-tab').forEach(t => t.classList.remove('active'));
    tab.classList.add('active');
  }
});
