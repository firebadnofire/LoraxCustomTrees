const fs = require('node:fs')
const path = require('node:path')
const mc = require('minecraft-protocol')
const diagnostics = []
const originalLog = console.log
console.log = (...args) => {
  const message = args.map(String).join(' ')
  if (message.includes('PartialReadError') || message.startsWith('Chunk size is ')) diagnostics.push(message)
  originalLog(...args)
}

const [portText, version, output] = process.argv.slice(2)
const port = Number(portText)
if (!Number.isInteger(port) || port < 1 || port > 65535 || !/^\d+\.\d+(\.\d+)?$/.test(version || '') || !output) {
  throw new Error('Usage: node join.cjs <loopback-port> <exact-Minecraft-version> <result.json>')
}
const clients = []
const joined = []
fs.mkdirSync(path.dirname(path.resolve(output)), { recursive: true })

function join(username) {
  return new Promise((resolve, reject) => {
    const client = mc.createClient({
      host: '127.0.0.1', port, version, username, auth: 'offline',
      profilesFolder: path.join(path.dirname(path.resolve(output)), 'client-profiles'),
      clientSettings: { viewDistance: 2 }
    })
    clients.push(client)
    let enteredPlay = false
    let spawned = false
    const timer = setTimeout(() => reject(new Error(`${username}: timed out entering the world`)), 45000)
    client.once('login', () => { enteredPlay = true })
    client.on('position', packet => {
      if (packet.teleportId !== undefined) client.write('teleport_confirm', { teleportId: packet.teleportId })
      if (!spawned && enteredPlay) {
        spawned = true
        clearTimeout(timer)
        joined.push(username)
        resolve()
      }
    })
    client.on('error', error => { diagnostics.push(error.stack); clearTimeout(timer); reject(error) })
    client.on('end', reason => {
      clearTimeout(timer)
      if (!spawned) reject(new Error(`${username}: disconnected before spawn (${reason})`))
    })
  })
}

async function run() {
  let result
  try {
    // Exercise two active players without conflating this with simultaneous-login stress.
    await join('loraxprobea')
    await join('loraxprobeb')
    await new Promise(resolve => setTimeout(resolve, 5000))
    if (clients.some(client => client.ended)) throw new Error('Client disconnected during multiplayer overlap')
    if (diagnostics.length) throw new Error('Client protocol decoding failed; see recorded diagnostics')
    result = { passed: true, minecraft: version, clients: joined, client_mods: [], handshake_order: 'sequential', simultaneous_seconds: 5 }
    console.log('VANILLA_PROTOCOL_JOIN_PASSED', JSON.stringify(result))
  } catch (error) {
    result = { passed: false, minecraft: version, clients: joined, error: error.stack, diagnostics }
    console.error('VANILLA_PROTOCOL_JOIN_FAILED', error)
    process.exitCode = 1
  } finally {
    for (const client of clients) client.end('Lorax integration complete')
    fs.writeFileSync(output, JSON.stringify(result, null, 2))
  }
}
run()
