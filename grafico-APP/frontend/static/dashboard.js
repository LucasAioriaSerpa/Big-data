let correlationChart;
let elbowChart;

function setStatus(message, isError = false) {
  const status = document.querySelector("#status");
  status.textContent = message;
  status.style.color = isError ? "#ff8a80" : "#a8b3c7";
}

function renderCards(results) {
  document.querySelector("#cards").innerHTML = results.map((item) => `
    <article class="card">
      <span>K=${item.k} · cluster=${item.cluster}</span>
      <strong>${item.records}</strong>
      <small>registros válidos</small>
    </article>
  `).join("");
}

function chartOptions(yTitle) {
  return {
    responsive: true,
    plugins: { legend: { labels: { color: "#d8e0ef" } } },
    scales: {
      x: { ticks: { color: "#a8b3c7" }, grid: { color: "#29344a" } },
      y: { title: { display: true, text: yTitle, color: "#d8e0ef" },
        ticks: { color: "#a8b3c7" }, grid: { color: "#29344a" } },
    },
  };
}

function renderElbow(points) {
  if (elbowChart) elbowChart.destroy();
  elbowChart = new Chart(document.querySelector("#elbow-chart"), {
    type: "line",
    data: {
      labels: points.map((point) => `K=${point.k}`),
      datasets: [{
        label: "Inércia (WCSS)",
        data: points.map((point) => point.inertia),
        borderColor: "#7c9cff",
        backgroundColor: "#7c9cff",
        pointRadius: 6,
        tension: 0.2,
      }],
    },
    options: chartOptions("Inércia"),
  });
}

function renderCorrelations(results) {
  const labels = results.flatMap((item) => item.correlations.map((entry) => entry.name));
  const uniqueLabels = [...new Set(labels)];
  if (correlationChart) correlationChart.destroy();
  correlationChart = new Chart(document.querySelector("#correlation-chart"), {
    type: "line",
    data: {
      labels: uniqueLabels,
      datasets: results.map((item, index) => ({
        label: `K=${item.k}, cluster=${item.cluster}`,
        data: item.correlations.map((entry) => entry.value),
        borderWidth: 2,
        borderColor: `hsl(${(index * 83) % 360} 70% 65%)`,
        spanGaps: true,
      })),
    },
    options: { ...chartOptions("Correlação"), scales: {
      ...chartOptions("Correlação").scales, y: {
        ...chartOptions("Correlação").scales.y, min: -1, max: 1,
      },
    } },
  });
}

async function loadResults() {
  const k = document.querySelector("#k-select").value;
  const resultsUrl = k ? `/api/results?k=${encodeURIComponent(k)}` : "/api/results";
  setStatus("Carregando resultados...");
  try {
    const [resultsResponse, elbowResponse] = await Promise.all([
      fetch(resultsUrl),
      fetch("/api/elbow"),
    ]);
    const results = await resultsResponse.json();
    const elbow = await elbowResponse.json();
    if (!resultsResponse.ok || !elbowResponse.ok) {
      throw new Error(results.error || elbow.error || "Falha ao carregar resultados.");
    }
    renderCards(results);
    renderElbow(elbow);
    renderCorrelations(results);
    setStatus(`${results.length} clusters carregados.`);
  } catch (error) {
    setStatus(error.message, true);
  }
}

document.querySelector("#load-button").addEventListener("click", loadResults);
loadResults();
