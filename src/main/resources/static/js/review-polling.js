(function () {
  var root = document.querySelector('[data-review-id][data-initial-status]');
  if (!root) return;
  var id = root.getAttribute('data-review-id');
  var initial = root.getAttribute('data-initial-status');
  if (!id || (initial !== 'PENDING' && initial !== 'LLM_EVALUATED')) return;
  var label = root.querySelector('.review-status-badge-label');
  if (!label) return;
  var badgeClasses = {
    'PENDING':       'badge bg-secondary d-inline-flex align-items-center gap-1',
    'LLM_EVALUATED': 'badge text-bg-warning d-inline-flex align-items-center gap-1',
    'APPROVED':      'badge text-bg-success d-inline-flex align-items-center gap-1',
    'REJECTED':      'badge text-bg-danger d-inline-flex align-items-center gap-1',
    'ERROR':         'badge border border-2 border-danger text-danger bg-body-secondary d-inline-flex align-items-center gap-1'
  };
  var intervalMs = 3000;
  var tick = function () {
    fetch('/reviews/' + encodeURIComponent(id) + '/status', { credentials: 'same-origin' })
      .then(function (r) {
        if (!r.ok) throw new Error(r.status);
        return r.json();
      })
      .then(function (data) {
        label.textContent = data.displayLabel;
        var badge = label.parentElement;
        if (badge && badgeClasses[data.status]) {
          badge.className = badgeClasses[data.status];
        }
        var spin = root.querySelector('.review-status-badge-spinner');
        if (spin) {
          if (data.status === 'LLM_EVALUATED') {
            spin.classList.remove('d-none');
            spin.setAttribute('aria-hidden', 'false');
          } else {
            spin.classList.add('d-none');
            spin.setAttribute('aria-hidden', 'true');
          }
        }
        if (data.status === 'APPROVED' || data.status === 'REJECTED' || data.status === 'ERROR') {
          clearInterval(handle);
        }
      })
      .catch(function () { /* next tick */ });
  };
  var handle = setInterval(tick, intervalMs);
  tick();
})();
